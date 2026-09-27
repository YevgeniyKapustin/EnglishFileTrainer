"""Клипы пунктов: расшифровка записей и подбор отрезков (plans/pipeline.md, шаг 5a).

    uv run --no-project --with faster-whisper python tools/clips.py transcribe audio/1_2.mp3 video/ep1_you_say.mp4
    uv run --no-project --with faster-whisper python tools/clips.py find audio/1_2.mp3 "Where do you live?" --after 12
    uv run --no-project --with faster-whisper python tools/clips.py dump audio/1_2.mp3

Пути — относительно app/src/main/assets. Расшифровка (пословные таймкоды faster-whisper small.en) и
карта громкости (где в записи звук) кешируются в tools/out/tr/ (не в git).

`find` ищет фразу в расшифровке, подтягивает границы к участкам звука (whisper после паузы часто
ставит начало на 1–2 с раньше), добавляет запас 0,3 с, не заходя за середину паузы до соседнего
звука, и печатает готовое `"clip": [начало, конец]` для JSON. В роликах с музыкой под диалогом
карта громкости ненадёжна — там `--words` (только таймкоды whisper). Результат прослушать: выравнивание
— подсказка, решение — за тем, кто пишет JSON. `dump` — вся расшифровка с таймкодами, чтобы найти
фразу глазами, если `find` промахнулся (например, цифры: whisper пишет «07700», в книге «oh seven…»).
"""
import argparse
import difflib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
CACHE = ROOT / "tools/out/tr"
MARGIN = 0.3


def cache_path(rel):
    return CACHE / (rel.replace("/", "_").rsplit(".", 1)[0] + ".json")


def transcribe(rel, model=None):
    p = cache_path(rel)
    if p.exists():
        return json.loads(p.read_text(encoding="utf-8"))
    from faster_whisper import WhisperModel
    from faster_whisper.audio import decode_audio
    import numpy as np

    model = model or WhisperModel("small.en", device="cpu", compute_type="int8")
    path = str(ASSETS / rel)
    segs, info = model.transcribe(path, word_timestamps=True, language="en", beam_size=5,
                                  vad_filter=False, condition_on_previous_text=False)
    words = [[round(w.start, 2), round(w.end, 2), w.word.strip()] for s in segs for w in s.words]

    # Карта громкости: окна 10 мс, порог от тихой части записи; соседние участки ближе 0,25 с сливаем.
    x = decode_audio(path, sampling_rate=16000)
    f = 160
    n = len(x) // f
    db = 20 * np.log10(np.sqrt((x[:n * f].reshape(n, f) ** 2).mean(1) + 1e-12))
    loud = db > max(np.percentile(db, 15) + 12, db.max() - 40)
    regions, i = [], 0
    while i < n:
        if loud[i]:
            j = i
            while j < n and loud[j]:
                j += 1
            if regions and i / 100 - regions[-1][1] < 0.25:
                regions[-1][1] = j / 100
            else:
                regions.append([i / 100, j / 100])
            i = j
        else:
            i += 1
    regions = [r for r in regions if r[1] - r[0] >= 0.08]

    data = {"duration": round(info.duration, 2), "words": words, "regions": regions}
    CACHE.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    return data


def tokens(text):
    return re.findall(r"[a-z0-9']+", text.lower().replace("’", "'").replace("-", " "))


def align(words, target, after):
    """Окно слов расшифровки, больше всего похожее на target (начало не раньше after)."""
    ws = [w for w in words if w[0] >= after - 0.01]
    flat, owner = [], []
    for i, w in enumerate(ws):
        for t in tokens(w[2]):
            flat.append(t)
            owner.append(i)
    tg = tokens(target)
    best = (0.0, None)
    for i in range(len(flat)):
        for size in range(max(1, len(tg) - 3), len(tg) + 4):
            if i + size > len(flat):
                break
            r = difflib.SequenceMatcher(None, flat[i:i + size], tg).ratio()
            if r > best[0]:
                best = (r, (owner[i], owner[i + size - 1]))
    if best[1] is None:
        return 0.0, []
    a, b = best[1]
    return best[0], ws[a:b + 1]


def clip_for(data, start, end, words_only):
    lo, hi = start - MARGIN, end + MARGIN
    regs = data["regions"]
    if not words_only and regs:
        # Участок звука — наш, если больше половины его внутри слов фразы (соседняя цифра/реплика
        # задевает растянутое whisper первое слово лишь краем).
        inside = [k for k, r in enumerate(regs)
                  if min(r[1], end) - max(r[0], start) > (r[1] - r[0]) / 2]
        if inside:
            a, b = inside[0], inside[-1]
            lo, hi = regs[a][0] - MARGIN, regs[b][1] + MARGIN
            if a > 0:
                lo = max(lo, (regs[a - 1][1] + regs[a][0]) / 2)
            if b < len(regs) - 1:
                hi = min(hi, (regs[b][1] + regs[b + 1][0]) / 2)
    return [round(max(lo, 0), 2), round(min(hi, data["duration"]), 2)]


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    t = sub.add_parser("transcribe")
    t.add_argument("files", nargs="+")
    f = sub.add_parser("find")
    f.add_argument("file")
    f.add_argument("text")
    f.add_argument("--after", type=float, default=0.0, help="искать не раньше этой секунды")
    f.add_argument("--words", action="store_true", help="без карты громкости (музыка под диалогом)")
    d = sub.add_parser("dump")
    d.add_argument("file")
    args = p.parse_args()

    if args.cmd == "transcribe":
        model = None
        for rel in args.files:
            if not cache_path(rel).exists() and model is None:
                from faster_whisper import WhisperModel
                model = WhisperModel("small.en", device="cpu", compute_type="int8")
            data = transcribe(rel, model)
            print(f"{rel}: {data['duration']} с, {len(data['words'])} слов, {len(data['regions'])} участков звука")
    elif args.cmd == "find":
        data = transcribe(args.file)
        score, ws = align(data["words"], args.text, args.after)
        if not ws:
            print("не найдено")
            return
        heard = " ".join(w[2] for w in ws)
        clip = clip_for(data, ws[0][0], ws[-1][1], args.words)
        print(f"совпадение {score:.2f}: «{heard}» {ws[0][0]}–{ws[-1][1]}")
        print(f'"clip": [{clip[0]}, {clip[1]}]')
    else:
        data = transcribe(args.file)
        line, t0 = [], None
        for s, e, w in data["words"]:
            if t0 is None:
                t0 = s
            line.append(w)
            if w.endswith((".", "?", "!")):
                print(f"{t0:7.2f}  {' '.join(line)}")
                line, t0 = [], None
        if line:
            print(f"{t0:7.2f}  {' '.join(line)}")


if __name__ == "__main__":
    main()
