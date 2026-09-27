import argparse
import difflib
import json
import re
from pathlib import Path
from typing import TYPE_CHECKING, Any, NamedTuple

if TYPE_CHECKING:
    from faster_whisper import WhisperModel

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
CACHE = ROOT / "tools/out/tr"
WHISPER_MODEL = "small.en"
MARGIN_SECONDS = 0.3
SAMPLE_RATE = 16000
LOUDNESS_WINDOWS_PER_SECOND = 100
QUIET_PERCENTILE = 15
LOUD_ABOVE_QUIET_DB = 12
LOUD_BELOW_PEAK_DB = 40
MERGE_GAP_SECONDS = 0.25
MIN_REGION_SECONDS = 0.08
AFTER_TOLERANCE_SECONDS = 0.01
WINDOW_SIZE_SLACK = 3
SENTENCE_ENDINGS = (".", "?", "!")


class Word(NamedTuple):
    start: float
    end: float
    text: str


class Region(NamedTuple):
    start: float
    end: float


class Transcript(NamedTuple):
    duration: float
    words: list[Word]
    regions: list[Region]

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> Transcript:
        return cls(
            duration=data["duration"],
            words=[Word(*word) for word in data["words"]],
            regions=[Region(*region) for region in data["regions"]],
        )

    def to_json(self) -> str:
        return json.dumps(
            {"duration": self.duration, "words": self.words, "regions": self.regions},
            ensure_ascii=False,
        )


def cache_path(rel: str) -> Path:
    return CACHE / (rel.replace("/", "_").rsplit(".", 1)[0] + ".json")


def load_model() -> WhisperModel:
    from faster_whisper import WhisperModel

    return WhisperModel(WHISPER_MODEL, device="cpu", compute_type="int8")


def merged_loud_runs(loud: list[bool]) -> list[Region]:
    regions: list[Region] = []
    index = 0
    while index < len(loud):
        if not loud[index]:
            index += 1
            continue
        run_end = index
        while run_end < len(loud) and loud[run_end]:
            run_end += 1
        start = index / LOUDNESS_WINDOWS_PER_SECOND
        end = run_end / LOUDNESS_WINDOWS_PER_SECOND
        if regions and start - regions[-1].end < MERGE_GAP_SECONDS:
            regions[-1] = Region(regions[-1].start, end)
        else:
            regions.append(Region(start, end))
        index = run_end
    return [region for region in regions if region.end - region.start >= MIN_REGION_SECONDS]


def sound_regions(path: str) -> list[Region]:
    import numpy as np
    from faster_whisper.audio import decode_audio

    samples = decode_audio(path, sampling_rate=SAMPLE_RATE)
    window = SAMPLE_RATE // LOUDNESS_WINDOWS_PER_SECOND
    count = len(samples) // window
    energy = (samples[: count * window].reshape(count, window) ** 2).mean(1)
    db = 20 * np.log10(np.sqrt(energy + 1e-12))
    threshold = max(np.percentile(db, QUIET_PERCENTILE) + LOUD_ABOVE_QUIET_DB, db.max() - LOUD_BELOW_PEAK_DB)
    return merged_loud_runs([bool(value) for value in db > threshold])


def transcribe(rel: str, model: WhisperModel | None = None) -> Transcript:
    cached = cache_path(rel)
    if cached.exists():
        return Transcript.from_json(json.loads(cached.read_text(encoding="utf-8")))
    model = model or load_model()
    path = str(ASSETS / rel)
    segments, info = model.transcribe(
        path,
        word_timestamps=True,
        language="en",
        beam_size=5,
        vad_filter=False,
        condition_on_previous_text=False,
    )
    words = [
        Word(round(word.start, 2), round(word.end, 2), word.word.strip())
        for segment in segments
        for word in segment.words
    ]
    transcript = Transcript(round(info.duration, 2), words, sound_regions(path))
    CACHE.mkdir(parents=True, exist_ok=True)
    cached.write_text(transcript.to_json(), encoding="utf-8")
    return transcript


def tokens(text: str) -> list[str]:
    return re.findall(r"[a-z0-9']+", text.lower().replace("’", "'").replace("-", " "))


def align(words: list[Word], target: str, after: float) -> tuple[float, list[Word]]:
    candidates = [word for word in words if word.start >= after - AFTER_TOLERANCE_SECONDS]
    flat: list[str] = []
    owner: list[int] = []
    for index, word in enumerate(candidates):
        for token in tokens(word.text):
            flat.append(token)
            owner.append(index)
    target_tokens = tokens(target)
    best_score = 0.0
    best_span: tuple[int, int] | None = None
    min_size = max(1, len(target_tokens) - WINDOW_SIZE_SLACK)
    max_size = len(target_tokens) + WINDOW_SIZE_SLACK
    for start in range(len(flat)):
        for size in range(min_size, max_size + 1):
            if start + size > len(flat):
                break
            score = difflib.SequenceMatcher(None, flat[start : start + size], target_tokens).ratio()
            if score > best_score:
                best_score = score
                best_span = (owner[start], owner[start + size - 1])
    if best_span is None:
        return 0.0, []
    first, last = best_span
    return best_score, candidates[first : last + 1]


def clip_for(transcript: Transcript, start: float, end: float, *, words_only: bool) -> tuple[float, float]:
    low, high = start - MARGIN_SECONDS, end + MARGIN_SECONDS
    regions = transcript.regions
    if not words_only and regions:
        inside = [
            index
            for index, region in enumerate(regions)
            if min(region.end, end) - max(region.start, start) > (region.end - region.start) / 2
        ]
        if inside:
            first, last = inside[0], inside[-1]
            low, high = regions[first].start - MARGIN_SECONDS, regions[last].end + MARGIN_SECONDS
            if first > 0:
                low = max(low, (regions[first - 1].end + regions[first].start) / 2)
            if last < len(regions) - 1:
                high = min(high, (regions[last].end + regions[last + 1].start) / 2)
    return round(max(low, 0), 2), round(min(high, transcript.duration), 2)


def run_transcribe(files: list[str]) -> None:
    model = None
    for rel in files:
        if not cache_path(rel).exists() and model is None:
            model = load_model()
        transcript = transcribe(rel, model)
        print(f"{rel}: {transcript.duration} с, {len(transcript.words)} слов, {len(transcript.regions)} участков звука")


def run_find(rel: str, text: str, after: float, *, words_only: bool) -> None:
    transcript = transcribe(rel)
    score, matched = align(transcript.words, text, after)
    if not matched:
        print("не найдено")
        return
    heard = " ".join(word.text for word in matched)
    clip_start, clip_end = clip_for(transcript, matched[0].start, matched[-1].end, words_only=words_only)
    print(f"совпадение {score:.2f}: «{heard}» {matched[0].start}–{matched[-1].end}")
    print(f'"clip": [{clip_start}, {clip_end}]')


def run_dump(rel: str) -> None:
    transcript = transcribe(rel)
    line: list[str] = []
    line_start: float | None = None
    for word in transcript.words:
        if line_start is None:
            line_start = word.start
        line.append(word.text)
        if word.text.endswith(SENTENCE_ENDINGS):
            print(f"{line_start:7.2f}  {' '.join(line)}")
            line, line_start = [], None
    if line:
        print(f"{line_start:7.2f}  {' '.join(line)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="cmd", required=True)
    transcribe_parser = commands.add_parser("transcribe")
    transcribe_parser.add_argument("files", nargs="+")
    find_parser = commands.add_parser("find")
    find_parser.add_argument("file")
    find_parser.add_argument("text")
    find_parser.add_argument("--after", type=float, default=0.0, help="искать не раньше этой секунды")
    find_parser.add_argument("--words", action="store_true", help="без карты громкости (музыка под диалогом)")
    dump_parser = commands.add_parser("dump")
    dump_parser.add_argument("file")
    args = parser.parse_args()

    if args.cmd == "transcribe":
        run_transcribe(args.files)
    elif args.cmd == "find":
        run_find(args.file, args.text, args.after, words_only=args.words)
    else:
        run_dump(args.file)


if __name__ == "__main__":
    main()
