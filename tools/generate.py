"""Генерирует data/content/File<ID>.kt из tools/content/<ID>.json (plans/pipeline.md, шаг 5).

    uv run --no-project python tools/generate.py 1A

Kotlin руками не правим: правка — в JSON, потом перегенерация.
Типы упражнений: blank, rewrite, speak (text, answers?, audio?; text из отдельных букв "A K E" —
мягкая проверка букв в Checker), talk (situation, goal, opener, criteria), write (prompt,
questions?, criteria — письменный ответ, проверяет ИИ). У таблицы правил — необязательный
"audio": трек Grammar Bank "1.4". У задания — необязательный "audio": трек SB "1.9" → assets/audio/1_9.mp3,
и "video": ролик Practical English "ep1_introduction" → assets/video/ep1_introduction.mp4.
id упражнения = "<task id>-<номер пункта в книге, 2 цифры>" и не меняется никогда.
У blank/rewrite/speak в задании с audio или video — необязательный "clip": [начало, конец] в секундах
от начала файла: пункт играет только этот отрезок (plans/pipeline.md, шаг «Клипы»). Длина файла проверяется
через mutagen, если он есть: uv run --no-project --with mutagen python tools/generate.py 1A.

Свежие упражнения-заготовки (plans/learning.md, «Свежие упражнения на правило»): задания раздела с
"origin": "GENERATED" и id "<раздел>-x<N>" ("1A-grammar-x1" → упражнения "1A-grammar-x1-01").
Сочинены заранее (source MODEL), только blank/rewrite, у каждого — explain. В «освоено» и
первый проход не входят, в тренировку идут через Review после освоения книжных пунктов раздела.
Проверка: не повторяют книгу и друг друга (по нормализованному тексту, как Normalizer.kt).
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "app/src/main/java/dev/eftrainer/data/content"
AUDIO_DIR = ROOT / "app/src/main/assets/audio"   # не в git — нет файла → только предупреждение
VIDEO_DIR = ROOT / "app/src/main/assets/video"   # тоже не в git

KINDS = {"GRAMMAR", "VOCABULARY", "PRONUNCIATION", "SKILLS", "PRACTICAL_ENGLISH"}
ORIGINS = {"WB", "SB", "SB_GRAMMAR_BANK", "GENERATED"}
SOURCES = {"KEY", "MODEL", "CONFIRMED"}


def s(v):
    """Строковый литерал Kotlin (или null)."""
    if v is None:
        return "null"
    return '"' + v.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def strings(xs):
    return "listOf(" + ", ".join(s(x) for x in xs) + ")"


def default_source(e):
    """Как в Model.kt: у Talk и Write по умолчанию CONFIRMED, у остальных KEY."""
    return "CONFIRMED" if e["type"] in ("talk", "write") else "KEY"


def audio_file(track):
    """Трек SB "1.9" → assets/audio/1_9.mp3."""
    return AUDIO_DIR / (track.replace(".", "_") + ".mp3")


def check_audio(owner, track, errors, warnings):
    if not re.fullmatch(r"\d+\.\d+", track):
        errors.append(f"{owner}: audio «{track}» — нужен номер трека вида 1.9")
    elif not audio_file(track).exists():
        warnings.append(f"{owner}: нет файла {audio_file(track).relative_to(ROOT)}")


def media_length(path):
    """Длина mp3/mp4 в секундах или None (нет файла или нет mutagen)."""
    if not path.exists():
        return None
    try:
        import mutagen
    except ImportError:
        return None
    f = mutagen.File(path)
    return f.info.length if f and f.info else None


def check_clip(eid, t, e, errors, warnings):
    """clip: [start, end] в секундах; только у пунктов задания с записью, внутри файла."""
    c = e["clip"]
    if not (isinstance(c, list) and len(c) == 2 and all(isinstance(x, (int, float)) for x in c)):
        errors.append(f"{eid}: clip — нужен [начало, конец] в секундах")
        return
    if e["type"] not in ("blank", "rewrite", "speak"):
        errors.append(f"{eid}: clip только у blank/rewrite/speak")
    if not (t.get("audio") or t.get("video")):
        errors.append(f"{eid}: clip без audio/video у задания")
        return
    start, end = c
    if not (0 <= start < end):
        errors.append(f"{eid}: clip {c} — нужно 0 ≤ начало < конец")
    path = audio_file(t["audio"]) if t.get("audio") else VIDEO_DIR / (t["video"] + ".mp4")
    length = media_length(path)
    if length is None:
        warnings.append(f"{eid}: длина {path.name} не проверена (нет файла или mutagen)")
    elif end > length + 0.05:
        errors.append(f"{eid}: clip {c} длиннее файла {path.name} ({length:.2f} с)")


def normalize(v):
    """Как Normalizer.kt: апострофы, пробелы, финальная . ? !, регистр."""
    v = re.sub(r"\s+", " ", v.replace("’", "'").replace("‘", "'")).strip()
    return re.sub(r"[.?!]+$", "", v).rstrip().lower()


def keys(e):
    """По чему ловим дубль (как GeneratePrompt.keys): текст задания и полный первый ответ.
    Шаблон вроде «What … ?» без подсказки не ключ — он общий у многих пунктов."""
    if e["type"] == "blank":
        parts = e["text"].split("{}")
        filled = "".join(p + (e["gaps"][i][0] if i < len(e["gaps"]) and e["gaps"][i] else "") for i, p in enumerate(parts))
        ks = [e["text"] + " " + (e.get("hint") or ""), filled]
    elif e["type"] == "rewrite":
        ks = [e.get("prefill"), e["answers"][0] if e["answers"] else None]
        if e.get("prompt") and not e.get("prefill"):
            ks.append(e["prompt"] + " " + (e.get("hint") or ""))
    else:
        ks = [e.get("text")]
    return {normalize(k) for k in ks if k}


def validate_presets(ch, errors):
    for sec in ch["sections"]:
        book = [e for t in sec["tasks"] if t["origin"] != "GENERATED" for e in t["exercises"]]
        seen = {k: "книга" for e in book for k in keys(e)}
        for t in sec["tasks"]:
            if t["origin"] != "GENERATED":
                continue
            if not re.fullmatch(re.escape(sec["id"]) + r"-x\d+", t["id"]):
                errors.append(f"{t['id']}: id заготовок — {sec['id']}-x<N>")
            if sec["kind"] not in ("GRAMMAR", "VOCABULARY"):
                errors.append(f"{t['id']}: заготовки — только в Grammar/Vocabulary")
            for e in t["exercises"]:
                eid = f"{t['id']}-{e['n']:02d}"
                if e["type"] not in ("blank", "rewrite"):
                    errors.append(f"{eid}: в заготовках только blank/rewrite")
                    continue
                if e.get("source") != "MODEL":
                    errors.append(f"{eid}: у заготовок source MODEL")
                if not (e.get("explain") or "").strip():
                    errors.append(f"{eid}: нет explain")
                for k in keys(e):
                    if k in seen:
                        errors.append(f"{eid}: повтор «{k}» ({seen[k]})")
                    seen[k] = eid


def validate(ch):
    """Возвращает (ошибки, предупреждения)."""
    errors = []
    warnings = []
    ids = set()

    def uniq(i):
        if i in ids:
            errors.append(f"повтор id {i}")
        ids.add(i)

    uniq(ch["id"])
    if "bubbles" in ch and (len(ch["bubbles"]) != 2 or not all(b.strip() for b in ch["bubbles"])):
        errors.append("bubbles: нужны две непустые реплики")
    for sec in ch["sections"]:
        uniq(sec["id"])
        if sec["kind"] not in KINDS:
            errors.append(f"{sec['id']}: kind {sec['kind']}")
        for r in sec["reference"]:
            if r["type"] == "table" and any(len(row) != len(r["headers"]) for row in r["rows"]):
                errors.append(f"{sec['id']}: строка таблицы «{r['title']}» не той ширины")
            if r["type"] == "table" and r.get("audio"):
                check_audio(f"{sec['id']} «{r['title']}»", r["audio"], errors, warnings)
        if not sec["tasks"]:
            errors.append(f"{sec['id']}: нет заданий")
        for t in sec["tasks"]:
            uniq(t["id"])
            if t["origin"] not in ORIGINS:
                errors.append(f"{t['id']}: origin {t['origin']}")
            if not t["exercises"]:
                errors.append(f"{t['id']}: нет упражнений")
            if t.get("audio"):
                check_audio(t["id"], t["audio"], errors, warnings)
            if t.get("video"):
                if not re.fullmatch(r"[a-z0-9_]+", t["video"]):
                    errors.append(f"{t['id']}: video «{t['video']}» — нужно имя вида ep1_introduction")
                elif not (VIDEO_DIR / (t["video"] + ".mp4")).exists():
                    warnings.append(f"{t['id']}: нет файла assets/video/{t['video']}.mp4")
            for e in t["exercises"]:
                eid = f"{t['id']}-{e['n']:02d}"
                uniq(eid)
                if e.get("source", default_source(e)) not in SOURCES:
                    errors.append(f"{eid}: source {e['source']}")
                if "clip" in e:
                    check_clip(eid, t, e, errors, warnings)
                if e["type"] == "blank":
                    if e["text"].count("{}") != len(e["gaps"]):
                        errors.append(f"{eid}: {{}} не совпадает с числом gaps")
                    variants = [v for g in e["gaps"] for v in g] + [None] * any(not g for g in e["gaps"])
                elif e["type"] == "rewrite":
                    if not (e.get("prompt") or e.get("prefill")):
                        errors.append(f"{eid}: нет ни prompt, ни prefill")
                    variants = e["answers"] or [None]
                elif e["type"] == "speak":
                    # answers можно не писать — тогда [text]
                    answers = e.get("answers") or [e["text"]]
                    if answers[0] != e["text"]:
                        errors.append(f"{eid}: answers[0] должен совпадать с text")
                    if e.get("audio"):
                        check_audio(eid, e["audio"], errors, warnings)
                    variants = [e["text"]] + answers
                elif e["type"] == "talk":
                    for f in ("situation", "goal", "opener"):
                        if not e.get(f, "").strip():
                            errors.append(f"{eid}: пустое поле {f}")
                    variants = e.get("criteria") or [None]
                elif e["type"] == "write":
                    if not e.get("prompt", "").strip():
                        errors.append(f"{eid}: пустое поле prompt")
                    variants = (e.get("criteria") or [None]) + e.get("questions", [])
                else:
                    errors.append(f"{eid}: тип {e['type']}")
                    continue
                if any(not v or not v.strip() for v in variants):
                    errors.append(f"{eid}: пустой ответ")
    validate_presets(ch, errors)
    return errors, warnings


def reference(r, ind):
    if r["type"] == "table":
        rows = f",\n{ind}        ".join(strings(row) for row in r["rows"])
        return (
            f"{ind}Reference.Table(\n"
            f"{ind}    title = {s(r['title'])},\n"
            f"{ind}    headers = {strings(r['headers'])},\n"
            f"{ind}    rows = listOf(\n{ind}        {rows},\n{ind}    ),\n"
            + (f"{ind}    audio = {s(r['audio'])},\n" if r.get("audio") else "")
            + f"{ind})"
        )
    if r["type"] == "note":
        return f"{ind}Reference.Note(title = {s(r.get('title'))}, text = {s(r['text'])})"
    if r["type"] == "mistake":
        return (
            f"{ind}Reference.Mistake(\n"
            f"{ind}    wrong = {s(r['wrong'])},\n"
            f"{ind}    right = {s(r['right'])},\n"
            f"{ind}    why = {s(r.get('why'))},\n"
            f"{ind})"
        )
    raise ValueError(r["type"])


def exercise(tid, e, ind):
    args = [s(f"{tid}-{e['n']:02d}")]
    if e["type"] == "blank":
        args.append(f"text = {s(e['text'])}")
        args.append("gaps = listOf(" + ", ".join(strings(g) for g in e["gaps"]) + ")")
        ctor = "Blank"
    elif e["type"] == "speak":
        args.append(f"text = {s(e['text'])}")
        args.append(f"answers = {strings(e.get('answers') or [e['text']])}")
        if e.get("audio"):
            args.append(f"audio = {s(e['audio'])}")
        ctor = "Speak"
    elif e["type"] == "talk":
        args.append(f"situation = {s(e['situation'])}")
        args.append(f"goal = {s(e['goal'])}")
        args.append(f"opener = {s(e['opener'])}")
        args.append(f"criteria = {strings(e['criteria'])}")
        ctor = "Talk"
    elif e["type"] == "write":
        args.append(f"prompt = {s(e['prompt'])}")
        if e.get("questions"):
            args.append(f"questions = {strings(e['questions'])}")
        args.append(f"criteria = {strings(e['criteria'])}")
        ctor = "Write"
    else:
        # С prefill prompt не показывается, но поле обязательное — дублируем фразу.
        args.append(f"prompt = {s(e.get('prompt') or e['prefill'])}")
        args.append(f"answers = {strings(e['answers'])}")
        if e.get("prefill"):
            args.append(f"prefill = {s(e['prefill'])}")
        ctor = "Rewrite"
    if e.get("hint"):
        args.append(f"hint = {s(e['hint'])}")
    if e.get("explain"):
        args.append(f"explain = {s(e['explain'])}")
    if e.get("source", default_source(e)) != default_source(e):
        args.append(f"source = AnswerSource.{e['source']}")
    if e.get("clip"):
        start, end = (round(x * 1000) for x in e["clip"])
        args.append(f"clip = Clip({start}, {end})")
    body = f",\n{ind}    ".join(args)
    return f"{ind}{ctor}(\n{ind}    {body},\n{ind})"


def task(t, ind):
    exs = ",\n".join(exercise(t["id"], e, ind + "        ") for e in t["exercises"])
    ctx = f"{ind}    context = {s(t['context'])},\n" if t.get("context") else ""
    audio = f"{ind}    audio = {s(t['audio'])},\n" if t.get("audio") else ""
    video = f"{ind}    video = {s(t['video'])},\n" if t.get("video") else ""
    return (
        f"{ind}Task(\n"
        f"{ind}    id = {s(t['id'])},\n"
        f"{ind}    origin = Origin.{t['origin']},\n"
        f"{ind}    instruction = {s(t['instruction'])},\n"
        f"{ctx}"
        f"{audio}"
        f"{video}"
        f"{ind}    exercises = listOf(\n{exs},\n{ind}    ),\n"
        f"{ind})"
    )


def items(xs, ind):
    """listOf(...) из уже отформатированных элементов; пустой — listOf()."""
    return "listOf(\n" + ",\n".join(xs) + f",\n{ind})" if xs else "listOf()"


def section(sec, ind):
    refs = [reference(r, ind + "        ") for r in sec.get("reference", [])]
    tasks = ",\n".join(task(t, ind + "        ") for t in sec["tasks"])
    return (
        f"{ind}Section(\n"
        f"{ind}    id = {s(sec['id'])},\n"
        f"{ind}    kind = SectionKind.{sec['kind']},\n"
        f"{ind}    title = {s(sec['title'])},\n"
        f"{ind}    reference = {items(refs, ind + '    ')},\n"
        f"{ind}    tasks = listOf(\n{tasks},\n{ind}    ),\n"
        f"{ind})"
    )


def generate(ch):
    name = "File" + ch["id"]
    sections = ",\n".join(section(sec, "        ") for sec in ch["sections"])
    b = ch.get("bubbles")
    bubbles = f"    bubbles = Bubbles({s(b[0])}, {s(b[1])}),\n" if b else ""
    return (
        "package dev.eftrainer.data.content\n\n"
        f"// СГЕНЕРИРОВАНО tools/generate.py из tools/content/{ch['id']}.json — руками не править.\n"
        f"// Источник: {ch.get('sources', '')}\n\n"
        f"val {name} = Chapter(\n"
        f"    id = {s(ch['id'])},\n"
        f"    title = {s(ch['title'])},\n"
        f"{bubbles}"
        f"    sections = listOf(\n{sections},\n    ),\n"
        ")\n"
    )


def main():
    cid = sys.argv[1]
    ch = json.loads((ROOT / f"tools/content/{cid}.json").read_text(encoding="utf-8"))
    errors, warnings = validate(ch)
    for w in warnings:
        print("Предупреждение:", w)
    if errors:
        sys.exit("Ошибки в JSON:\n  " + "\n  ".join(errors))
    out = OUT_DIR / f"File{cid}.kt"
    out.write_text(generate(ch), encoding="utf-8", newline="\n")
    n = sum(len(t["exercises"]) for sec in ch["sections"] for t in sec["tasks"] if t["origin"] != "GENERATED")
    x = sum(len(t["exercises"]) for sec in ch["sections"] for t in sec["tasks"] if t["origin"] == "GENERATED")
    print(f"{out.relative_to(ROOT)}: {len(ch['sections'])} разд., {n} упр. + {x} заготовок")


if __name__ == "__main__":
    main()
