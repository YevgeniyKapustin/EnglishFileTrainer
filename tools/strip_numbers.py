import argparse
import json
import re
import shutil
from pathlib import Path
from typing import TYPE_CHECKING, Any, NamedTuple

from clips import ASSETS, SAMPLE_RATE, Region, cache_path, load_model, sound_regions, tokens
from media import AUDIO_ARGS, ffmpeg

if TYPE_CHECKING:
    import numpy as np
    from faster_whisper import WhisperModel

ROOT = Path(__file__).resolve().parent.parent
CONTENT_DIR = ROOT / "tools/content"
MANIFEST = ROOT / "tools/audio_cuts.json"
SOURCE_DIR = ROOT / "english_file/audio"
BACKUP_DIR = ROOT / "tools/out/audio_original"
ANNOUNCEMENT_MAX_START_SECONDS = 0.3
ANNOUNCEMENT_MAX_SECONDS = 2.2
LABEL_MAX_SECONDS = 1.2
MIN_SILENCE_SECONDS = 0.4
LABEL_MIN_PAUSE_BEFORE_SECONDS = 0.85
HEARING_MARGIN_SECONDS = 0.15
CUT_PAD_SECONDS = 0.1
LEAD_SECONDS = 0.3
ANNOUNCEMENT_AND_NEXT = 2
TRACKS_WHERE_ITEM_NUMBERS_ANSWER_THE_TASK = {"1.10", "1.11", "2.15"}
DIGITS = re.compile(r"^\d+$")
UNITS = ["one", "two", "three", "four", "five", "six", "seven", "eight", "nine"]
TEENS = ["ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"]
TENS = ["twenty", "thirty", "forty", "fifty"]
NUMBER_WORDS = set(UNITS + TEENS + TENS)
NUMBER_HOMOPHONES = {"won", "to", "too", "for", "ate"}
LABEL_PREFIX = "number"
COMPOUND_NUMBER_WORDS = 2
ANNOUNCEMENT_EXTRAS = {"point", "track", "and", "oh"}


class ClipCountMismatchError(RuntimeError):
    def __init__(self, path: Path, matched: int, parsed: int) -> None:
        super().__init__(f"{path.name}: {matched} clip matches, {parsed} clips")


class Cut(NamedTuple):
    start: float
    end: float
    heard: str


def is_number(token: str) -> bool:
    return bool(DIGITS.match(token)) or token in NUMBER_WORDS or token in NUMBER_HOMOPHONES


def is_item_label(heard: str) -> bool:
    words = tokens(heard)
    if words[:1] == [LABEL_PREFIX]:
        words = words[1:]
    if len(words) == 1:
        return is_number(words[0])
    return len(words) == COMPOUND_NUMBER_WORDS and words[0] in TENS and words[1] in UNITS


def is_announcement(heard: str) -> bool:
    words = tokens(heard)
    return (
        bool(words)
        and any(is_number(word) for word in words)
        and all(is_number(word) or word in ANNOUNCEMENT_EXTRAS for word in words)
    )


def track_file(track: str) -> Path:
    return ASSETS / "audio" / (track.replace(".", "_") + ".ogg")


def track_rel(track: str) -> str:
    return "audio/" + track.replace(".", "_") + ".ogg"


def load_samples(path: Path) -> np.ndarray:
    from faster_whisper.audio import decode_audio

    samples: np.ndarray = decode_audio(str(path), sampling_rate=SAMPLE_RATE)
    return samples


def hear(model: WhisperModel, samples: np.ndarray, start: float, end: float) -> str:
    chunk = samples[int(start * SAMPLE_RATE) : int(end * SAMPLE_RATE)]
    segments, _ = model.transcribe(
        chunk,
        language="en",
        beam_size=5,
        vad_filter=False,
        condition_on_previous_text=False,
        without_timestamps=True,
    )
    return " ".join(segment.text.strip() for segment in segments).strip()


def announcement_cut(model: WhisperModel, samples: np.ndarray, regions: list[Region]) -> Cut | None:
    if len(regions) < ANNOUNCEMENT_AND_NEXT:
        return None
    first, following = regions[0], regions[1]
    if first.start > ANNOUNCEMENT_MAX_START_SECONDS or first.end - first.start > ANNOUNCEMENT_MAX_SECONDS:
        return None
    if following.start - first.end < MIN_SILENCE_SECONDS:
        return None
    heard = hear(model, samples, 0, min(first.end + HEARING_MARGIN_SECONDS, following.start))
    if not is_announcement(heard):
        return None
    return Cut(0.0, round(following.start - LEAD_SECONDS, 2), heard)


class Label(NamedTuple):
    cut: Cut
    pause_before: float
    pause_after: float


def label_candidates(model: WhisperModel, samples: np.ndarray, regions: list[Region]) -> list[Label]:
    labels = []
    for previous, region, following in zip(regions, regions[1:], regions[2:], strict=False):
        pause_before, pause_after = region.start - previous.end, following.start - region.end
        if region.end - region.start > LABEL_MAX_SECONDS:
            continue
        if pause_before < LABEL_MIN_PAUSE_BEFORE_SECONDS or pause_after < MIN_SILENCE_SECONDS:
            continue
        heard = hear(
            model,
            samples,
            max(region.start - HEARING_MARGIN_SECONDS, previous.end),
            min(region.end + HEARING_MARGIN_SECONDS, following.start),
        )
        if is_item_label(heard):
            start = max(region.start - CUT_PAD_SECONDS, previous.end + CUT_PAD_SECONDS)
            cut = Cut(round(start, 2), round(following.start - LEAD_SECONDS, 2), heard)
            labels.append(Label(cut, pause_before, pause_after))
    return labels


def numbers_follow_their_items(labels: list[Label]) -> bool:
    trailing = sum(label.pause_after > label.pause_before for label in labels)
    return trailing * 2 > len(labels)


def find_cuts(model: WhisperModel, track: str) -> list[Cut]:
    path = track_file(track)
    samples = load_samples(path)
    regions = sound_regions(str(path))
    leading = announcement_cut(model, samples, regions)
    cuts = [leading] if leading else []
    if track in TRACKS_WHERE_ITEM_NUMBERS_ANSWER_THE_TASK:
        return cuts
    labels = label_candidates(model, samples, regions if leading else [Region(0, 0), *regions])
    if numbers_follow_their_items(labels):
        return cuts
    return cuts + [label.cut for label in labels]


def shift(time: float, cuts: list[Cut]) -> float:
    removed = sum(max(0.0, min(time, cut.end) - cut.start) for cut in cuts if time > cut.start)
    return round(time - removed, 2)


def source_for(track: str) -> Path:
    original = SOURCE_DIR / f"EF4e_PreintSB_{track}.mp3"
    if original.exists():
        return original
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    backup = BACKUP_DIR / track_file(track).name
    if not backup.exists():
        shutil.copy2(track_file(track), backup)
    return backup


def cut_audio(track: str, cuts: list[Cut]) -> None:
    kept = []
    position = 0.0
    for cut in cuts:
        kept.append(f"atrim=start={position}:end={cut.start}")
        position = cut.end
    kept.append(f"atrim=start={position}")
    parts = [f"[0:a]{trim},asetpts=PTS-STARTPTS[part{index}]" for index, trim in enumerate(kept)]
    joined = "".join(f"[part{index}]" for index in range(len(kept)))
    graph = ";".join([*parts, f"{joined}concat=n={len(kept)}:v=0:a=1[out]"])
    target = track_file(track)
    temporary = target.with_suffix(".tmp.ogg")
    ffmpeg("-i", str(source_for(track)), "-filter_complex", graph, "-map", "[out]", *AUDIO_ARGS, str(temporary))
    temporary.replace(target)


def clip_owners(node: object, media: str | None, found: list[tuple[str | None, dict[str, Any]]]) -> None:
    if isinstance(node, dict):
        if "audio" in node:
            media = node["audio"]
        elif "video" in node:
            media = None
        if "clip" in node:
            found.append((media, node))
        for value in node.values():
            clip_owners(value, media, found)
    elif isinstance(node, list):
        for value in node:
            clip_owners(value, media, found)


CLIP_PATTERN = re.compile(r'("clip":\s*\[\s*)([\d.]+)(\s*,\s*)([\d.]+)(\s*\])')


def remap_clips(cuts_by_track: dict[str, list[Cut]]) -> list[str]:
    changed_chapters = []
    for path in sorted(CONTENT_DIR.glob("*.json")):
        text = path.read_text(encoding="utf-8")
        owners: list[tuple[str | None, dict[str, Any]]] = []
        clip_owners(json.loads(text), None, owners)
        matches = list(CLIP_PATTERN.finditer(text))
        if len(matches) != len(owners):
            raise ClipCountMismatchError(path, len(matches), len(owners))
        pieces = []
        position = 0
        for match, (track, _) in zip(matches, owners, strict=True):
            cuts = cuts_by_track.get(track or "")
            if not cuts:
                continue
            start, end = shift(float(match[2]), cuts), shift(float(match[4]), cuts)
            pieces.append(text[position : match.start()])
            pieces.append(f"{match[1]}{start}{match[3]}{end}{match[5]}")
            position = match.end()
        if pieces:
            path.write_text("".join(pieces) + text[position:], encoding="utf-8")
            changed_chapters.append(path.stem)
    return changed_chapters


def all_tracks() -> list[str]:
    names = (ASSETS / "audio").glob("*.ogg")
    present = {path.stem.replace("_", ".") for path in names if ".tmp" not in path.name}
    return sorted(present | set(load_manifest()))


def load_manifest() -> dict[str, list[list[Any]]]:
    return json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}


def describe(track: str, cuts: list[Cut]) -> str:
    listed = ", ".join(f"«{cut.heard}» {cut.start}–{cut.end}" for cut in cuts) or "nothing"
    return f"{track}: {listed}"


def run_strip(tracks: list[str], *, dry_run: bool) -> None:
    manifest = load_manifest()
    model = None
    stripped: dict[str, list[Cut]] = {}
    for track in tracks:
        if track in manifest:
            if track_file(track).exists():
                print(f"{track}: already stripped")
            elif not dry_run:
                cut_audio(track, [Cut(*cut) for cut in manifest[track]])
                print(f"{track}: rebuilt from the original with the recorded cuts")
            continue
        model = model or load_model()
        cuts = find_cuts(model, track)
        print(describe(track, cuts))
        if dry_run:
            continue
        if cuts:
            cut_audio(track, cuts)
            cache_path(track_rel(track)).unlink(missing_ok=True)
            stripped[track] = cuts
        manifest[track] = [list(cut) for cut in cuts]
        MANIFEST.write_text(json.dumps(dict(sorted(manifest.items())), ensure_ascii=False, indent=2) + "\n", "utf-8")
    if stripped:
        print("clips remapped in: " + ", ".join(remap_clips(stripped)))


def run_check(tracks: list[str]) -> None:
    model = load_model()
    for track in tracks:
        print(describe(track, find_cuts(model, track)))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("tracks", nargs="*")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    tracks = args.tracks or all_tracks()
    if args.check:
        run_check(tracks)
    else:
        run_strip(tracks, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
