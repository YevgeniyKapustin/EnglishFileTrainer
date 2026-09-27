import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from collections.abc import Callable, Iterable

type Json = dict[str, Any]

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "app/src/main/java/dev/eftrainer/data/content"
AUDIO_DIR = ROOT / "app/src/main/assets/audio"
VIDEO_DIR = ROOT / "app/src/main/assets/video"
IMAGE_DIR = ROOT / "app/src/main/assets/image"

KINDS = {"GRAMMAR", "VOCABULARY", "PRONUNCIATION", "SKILLS", "PRACTICAL_ENGLISH", "REVIEW"}
ORIGINS = {"WB", "SB", "SB_GRAMMAR_BANK", "GENERATED"}
SOURCES = {"KEY", "MODEL", "CONFIRMED"}
AI_CHECKED_TYPES = ("talk", "write")
CLIPPABLE_TYPES = ("blank", "rewrite", "speak")
IMAGE_TYPES = ("blank", "rewrite")
PRESET_TYPES = ("blank", "rewrite")
PRESET_SECTION_KINDS = ("GRAMMAR", "VOCABULARY")
CLIP_BOUNDS_COUNT = 2
BUBBLE_COUNT = 2
CLIP_LENGTH_TOLERANCE_SECONDS = 0.05


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    ids: set[str] = field(default_factory=set)

    def claim_id(self, item_id: str) -> None:
        if item_id in self.ids:
            self.errors.append(f"повтор id {item_id}")
        self.ids.add(item_id)


def kotlin_string(value: str | None) -> str:
    if value is None:
        return "null"
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kotlin_strings(values: Iterable[str]) -> str:
    return "listOf(" + ", ".join(kotlin_string(value) for value in values) + ")"


def kotlin_list(formatted_items: list[str], indent: str) -> str:
    return "listOf(\n" + ",\n".join(formatted_items) + f",\n{indent})" if formatted_items else "listOf()"


def exercise_id(task: Json, exercise: Json) -> str:
    return f"{task['id']}-{exercise['n']:02d}"


def default_source(exercise: Json) -> str:
    return "CONFIRMED" if exercise["type"] in AI_CHECKED_TYPES else "KEY"


def audio_path(track: str) -> Path:
    base = AUDIO_DIR / track.replace(".", "_")
    compressed = base.with_suffix(".ogg")
    return compressed if compressed.exists() else base.with_suffix(".mp3")


def video_path(name: str) -> Path:
    return VIDEO_DIR / (name + ".mp4")


def check_image(owner: str, name: str, report: Report) -> None:
    if not re.fullmatch(r"[a-z0-9_]+", name):
        report.errors.append(f"{owner}: image «{name}» — нужно имя вида 1b_vb1a_2")
    elif not (IMAGE_DIR / f"{name}.webp").exists():
        report.warnings.append(f"{owner}: нет файла assets/image/{name}.webp")


def check_audio(owner: str, track: str, report: Report) -> None:
    if not re.fullmatch(r"\d+\.\d+", track):
        report.errors.append(f"{owner}: audio «{track}» — нужен номер трека вида 1.9")
    elif not audio_path(track).exists():
        report.warnings.append(f"{owner}: нет файла {audio_path(track).relative_to(ROOT)}")


def media_length_seconds(path: Path) -> float | None:
    if not path.exists():
        return None
    try:
        import mutagen
    except ImportError:
        return None
    media = mutagen.File(path)
    return float(media.info.length) if media and media.info else None


def check_clip(eid: str, task: Json, exercise: Json, report: Report) -> None:
    clip = exercise["clip"]
    is_pair_of_numbers = (
        isinstance(clip, list)
        and len(clip) == CLIP_BOUNDS_COUNT
        and all(isinstance(bound, int | float) for bound in clip)
    )
    if not is_pair_of_numbers:
        report.errors.append(f"{eid}: clip — нужен [начало, конец] в секундах")
        return
    if exercise["type"] not in CLIPPABLE_TYPES:
        report.errors.append(f"{eid}: clip только у blank/rewrite/speak")
    if not (task.get("audio") or task.get("video")):
        report.errors.append(f"{eid}: clip без audio/video у задания")
        return
    start, end = clip
    if not (0 <= start < end):
        report.errors.append(f"{eid}: clip {clip} — нужно 0 ≤ начало < конец")
    path = audio_path(task["audio"]) if task.get("audio") else video_path(task["video"])
    length = media_length_seconds(path)
    if length is None:
        report.warnings.append(f"{eid}: длина {path.name} не проверена (нет файла или mutagen)")
    elif end > length + CLIP_LENGTH_TOLERANCE_SECONDS:
        report.errors.append(f"{eid}: clip {clip} длиннее файла {path.name} ({length:.2f} с)")


def normalize(text: str) -> str:
    text = re.sub(r"\s+", " ", text.replace("’", "'").replace("‘", "'")).strip()
    return re.sub(r"[.?!]+$", "", text).rstrip().lower()


def filled_blank(exercise: Json) -> str:
    parts = exercise["text"].split("{}")
    gaps = exercise["gaps"]
    return "".join(
        part + (gaps[index][0] if index < len(gaps) and gaps[index] else "") for index, part in enumerate(parts)
    )


def duplicate_keys(exercise: Json) -> set[str]:
    candidates: list[str | None]
    if exercise["type"] == "blank":
        candidates = [exercise["text"] + " " + (exercise.get("hint") or ""), filled_blank(exercise)]
    elif exercise["type"] == "rewrite":
        candidates = [exercise.get("prefill"), exercise["answers"][0] if exercise["answers"] else None]
        if exercise.get("prompt") and not exercise.get("prefill"):
            candidates.append(exercise["prompt"] + " " + (exercise.get("hint") or ""))
    else:
        candidates = [exercise.get("text")]
    return {normalize(candidate) for candidate in candidates if candidate}


def validate_preset_task(section: Json, task: Json, seen: dict[str, str], report: Report) -> None:
    if not re.fullmatch(re.escape(section["id"]) + r"-x\d+", task["id"]):
        report.errors.append(f"{task['id']}: id заготовок — {section['id']}-x<N>")
    if section["kind"] not in PRESET_SECTION_KINDS:
        report.errors.append(f"{task['id']}: заготовки — только в Grammar/Vocabulary")
    for exercise in task["exercises"]:
        eid = exercise_id(task, exercise)
        if exercise["type"] not in PRESET_TYPES:
            report.errors.append(f"{eid}: в заготовках только blank/rewrite")
            continue
        if exercise.get("source") != "MODEL":
            report.errors.append(f"{eid}: у заготовок source MODEL")
        if not (exercise.get("explain") or "").strip():
            report.errors.append(f"{eid}: нет explain")
        for key in duplicate_keys(exercise):
            if key in seen:
                report.errors.append(f"{eid}: повтор «{key}» ({seen[key]})")
            seen[key] = eid


def validate_presets(chapter: Json, report: Report) -> None:
    for section in chapter["sections"]:
        book = [
            exercise for task in section["tasks"] if task["origin"] != "GENERATED" for exercise in task["exercises"]
        ]
        seen = {key: "книга" for exercise in book for key in duplicate_keys(exercise)}
        for task in section["tasks"]:
            if task["origin"] == "GENERATED":
                validate_preset_task(section, task, seen, report)


def blank_answers(eid: str, exercise: Json, report: Report) -> list[str | None]:
    if exercise["text"].count("{}") != len(exercise["gaps"]):
        report.errors.append(f"{eid}: {{}} не совпадает с числом gaps")
    has_empty_gap = any(not gap for gap in exercise["gaps"])
    return [variant for gap in exercise["gaps"] for variant in gap] + [None] * has_empty_gap


def rewrite_answers(eid: str, exercise: Json, report: Report) -> list[str | None]:
    if not (exercise.get("prompt") or exercise.get("prefill")):
        report.errors.append(f"{eid}: нет ни prompt, ни prefill")
    answers: list[str | None] = exercise["answers"] or [None]
    return answers


def speak_answers(eid: str, exercise: Json, report: Report) -> list[str | None]:
    answers = exercise.get("answers") or [exercise["text"]]
    if answers[0] != exercise["text"]:
        report.errors.append(f"{eid}: answers[0] должен совпадать с text")
    if exercise.get("audio"):
        check_audio(eid, exercise["audio"], report)
    return [exercise["text"], *answers]


def talk_answers(eid: str, exercise: Json, report: Report) -> list[str | None]:
    for field_name in ("situation", "goal", "opener"):
        if not exercise.get(field_name, "").strip():
            report.errors.append(f"{eid}: пустое поле {field_name}")
    return exercise.get("criteria") or [None]


def write_answers(eid: str, exercise: Json, report: Report) -> list[str | None]:
    if not exercise.get("prompt", "").strip():
        report.errors.append(f"{eid}: пустое поле prompt")
    criteria: list[str | None] = exercise.get("criteria") or [None]
    questions: list[str] = exercise.get("questions", [])
    return criteria + questions


def order_answers(eid: str, exercise: Json, report: Report) -> list[str | None]:
    labels = [picture["label"] for picture in exercise["pictures"]]
    if len(set(labels)) != len(labels) or sorted(exercise["answer"]) != sorted(labels):
        report.errors.append(f"{eid}: answer должен быть перестановкой меток картинок")
    for picture in exercise["pictures"]:
        check_image(eid, picture["image"], report)
    return [*labels, *(picture["caption"] for picture in exercise["pictures"])] or [None]


ANSWER_VALIDATORS: dict[str, Callable[[str, Json, Report], list[str | None]]] = {
    "blank": blank_answers,
    "rewrite": rewrite_answers,
    "speak": speak_answers,
    "talk": talk_answers,
    "write": write_answers,
    "order": order_answers,
}


def validate_exercise(task: Json, exercise: Json, report: Report) -> None:
    eid = exercise_id(task, exercise)
    report.claim_id(eid)
    if exercise.get("source", default_source(exercise)) not in SOURCES:
        report.errors.append(f"{eid}: source {exercise['source']}")
    if "clip" in exercise:
        check_clip(eid, task, exercise, report)
    if "image" in exercise:
        if exercise["type"] not in IMAGE_TYPES:
            report.errors.append(f"{eid}: image только у blank/rewrite")
        check_image(eid, exercise["image"], report)
    validate_answers = ANSWER_VALIDATORS.get(exercise["type"])
    if validate_answers is None:
        report.errors.append(f"{eid}: тип {exercise['type']}")
        return
    if any(not answer or not answer.strip() for answer in validate_answers(eid, exercise, report)):
        report.errors.append(f"{eid}: пустой ответ")


def validate_video(task: Json, report: Report) -> None:
    if not re.fullmatch(r"[a-z0-9_]+", task["video"]):
        report.errors.append(f"{task['id']}: video «{task['video']}» — нужно имя вида ep1_introduction")
    elif not video_path(task["video"]).exists():
        report.warnings.append(f"{task['id']}: нет файла assets/video/{task['video']}.mp4")


def validate_task(task: Json, report: Report) -> None:
    report.claim_id(task["id"])
    if task["origin"] not in ORIGINS:
        report.errors.append(f"{task['id']}: origin {task['origin']}")
    if not task["exercises"]:
        report.errors.append(f"{task['id']}: нет упражнений")
    if task.get("audio"):
        check_audio(task["id"], task["audio"], report)
    if task.get("video"):
        validate_video(task, report)
    for exercise in task["exercises"]:
        validate_exercise(task, exercise, report)


def validate_reference(section: Json, reference: Json, report: Report) -> None:
    if reference["type"] != "table":
        return
    if any(len(row) != len(reference["headers"]) for row in reference["rows"]):
        report.errors.append(f"{section['id']}: строка таблицы «{reference['title']}» не той ширины")
    if reference.get("audio"):
        check_audio(f"{section['id']} «{reference['title']}»", reference["audio"], report)


def validate_section(section: Json, report: Report) -> None:
    report.claim_id(section["id"])
    if section["kind"] not in KINDS:
        report.errors.append(f"{section['id']}: kind {section['kind']}")
    for reference in section["reference"]:
        validate_reference(section, reference, report)
    if not section["tasks"]:
        report.errors.append(f"{section['id']}: нет заданий")
    for task in section["tasks"]:
        validate_task(task, report)


def validate(chapter: Json) -> Report:
    report = Report()
    report.claim_id(chapter["id"])
    bubbles = chapter.get("bubbles")
    if bubbles is not None and (len(bubbles) != BUBBLE_COUNT or not all(bubble.strip() for bubble in bubbles)):
        report.errors.append("bubbles: нужны две непустые реплики")
    for section in chapter["sections"]:
        validate_section(section, report)
    validate_presets(chapter, report)
    return report


def reference_code(reference: Json, indent: str) -> str:
    if reference["type"] == "table":
        rows = f",\n{indent}        ".join(kotlin_strings(row) for row in reference["rows"])
        return (
            f"{indent}Reference.Table(\n"
            f"{indent}    title = {kotlin_string(reference['title'])},\n"
            f"{indent}    headers = {kotlin_strings(reference['headers'])},\n"
            f"{indent}    rows = listOf(\n{indent}        {rows},\n{indent}    ),\n"
            + (f"{indent}    audio = {kotlin_string(reference['audio'])},\n" if reference.get("audio") else "")
            + f"{indent})"
        )
    if reference["type"] == "note":
        return (
            f"{indent}Reference.Note(title = {kotlin_string(reference.get('title'))}, "
            f"text = {kotlin_string(reference['text'])})"
        )
    if reference["type"] == "mistake":
        return (
            f"{indent}Reference.Mistake(\n"
            f"{indent}    wrong = {kotlin_string(reference['wrong'])},\n"
            f"{indent}    right = {kotlin_string(reference['right'])},\n"
            f"{indent}    why = {kotlin_string(reference.get('why'))},\n"
            f"{indent})"
        )
    raise ValueError(reference["type"])


def blank_arguments(exercise: Json) -> list[str]:
    return [
        f"text = {kotlin_string(exercise['text'])}",
        "gaps = listOf(" + ", ".join(kotlin_strings(gap) for gap in exercise["gaps"]) + ")",
    ]


def speak_arguments(exercise: Json) -> list[str]:
    arguments = [
        f"text = {kotlin_string(exercise['text'])}",
        f"answers = {kotlin_strings(exercise.get('answers') or [exercise['text']])}",
    ]
    if exercise.get("audio"):
        arguments.append(f"audio = {kotlin_string(exercise['audio'])}")
    return arguments


def talk_arguments(exercise: Json) -> list[str]:
    return [
        f"situation = {kotlin_string(exercise['situation'])}",
        f"goal = {kotlin_string(exercise['goal'])}",
        f"opener = {kotlin_string(exercise['opener'])}",
        f"criteria = {kotlin_strings(exercise['criteria'])}",
    ]


def write_arguments(exercise: Json) -> list[str]:
    arguments = [f"prompt = {kotlin_string(exercise['prompt'])}"]
    if exercise.get("questions"):
        arguments.append(f"questions = {kotlin_strings(exercise['questions'])}")
    arguments.append(f"criteria = {kotlin_strings(exercise['criteria'])}")
    return arguments


def rewrite_arguments(exercise: Json) -> list[str]:
    arguments = [
        f"prompt = {kotlin_string(exercise.get('prompt') or exercise['prefill'])}",
        f"answers = {kotlin_strings(exercise['answers'])}",
    ]
    if exercise.get("prefill"):
        arguments.append(f"prefill = {kotlin_string(exercise['prefill'])}")
    return arguments


def order_arguments(exercise: Json) -> list[str]:
    pictures = ", ".join(
        f"Picture({kotlin_string(p['label'])}, {kotlin_string(p['image'])}, {kotlin_string(p['caption'])})"
        for p in exercise["pictures"]
    )
    return [f"pictures = listOf({pictures})", f"answer = {kotlin_strings(exercise['answer'])}"]


CONSTRUCTORS: dict[str, tuple[str, Callable[[Json], list[str]]]] = {
    "blank": ("Blank", blank_arguments),
    "speak": ("Speak", speak_arguments),
    "talk": ("Talk", talk_arguments),
    "write": ("Write", write_arguments),
    "order": ("Order", order_arguments),
}


def common_arguments(exercise: Json) -> list[str]:
    arguments = []
    if exercise.get("image"):
        arguments.append(f"image = {kotlin_string(exercise['image'])}")
    if exercise.get("hint"):
        arguments.append(f"hint = {kotlin_string(exercise['hint'])}")
    if exercise.get("explain"):
        arguments.append(f"explain = {kotlin_string(exercise['explain'])}")
    if exercise.get("source", default_source(exercise)) != default_source(exercise):
        arguments.append(f"source = AnswerSource.{exercise['source']}")
    if exercise.get("clip"):
        start, end = (round(seconds * 1000) for seconds in exercise["clip"])
        arguments.append(f"clip = Clip({start}, {end})")
    return arguments


def exercise_code(task: Json, exercise: Json, indent: str) -> str:
    constructor, type_arguments = CONSTRUCTORS.get(exercise["type"], ("Rewrite", rewrite_arguments))
    arguments = [
        kotlin_string(exercise_id(task, exercise)),
        *type_arguments(exercise),
        *common_arguments(exercise),
    ]
    body = f",\n{indent}    ".join(arguments)
    return f"{indent}{constructor}(\n{indent}    {body},\n{indent})"


def task_code(task: Json, indent: str) -> str:
    exercises = ",\n".join(exercise_code(task, exercise, indent + "        ") for exercise in task["exercises"])
    context = f"{indent}    context = {kotlin_string(task['context'])},\n" if task.get("context") else ""
    audio = f"{indent}    audio = {kotlin_string(task['audio'])},\n" if task.get("audio") else ""
    video = f"{indent}    video = {kotlin_string(task['video'])},\n" if task.get("video") else ""
    return (
        f"{indent}Task(\n"
        f"{indent}    id = {kotlin_string(task['id'])},\n"
        f"{indent}    origin = Origin.{task['origin']},\n"
        f"{indent}    instruction = {kotlin_string(task['instruction'])},\n"
        f"{context}"
        f"{audio}"
        f"{video}"
        f"{indent}    exercises = listOf(\n{exercises},\n{indent}    ),\n"
        f"{indent})"
    )


def section_code(section: Json, indent: str) -> str:
    references = [reference_code(reference, indent + "        ") for reference in section.get("reference", [])]
    tasks = ",\n".join(task_code(task, indent + "        ") for task in section["tasks"])
    return (
        f"{indent}Section(\n"
        f"{indent}    id = {kotlin_string(section['id'])},\n"
        f"{indent}    kind = SectionKind.{section['kind']},\n"
        f"{indent}    title = {kotlin_string(section['title'])},\n"
        f"{indent}    reference = {kotlin_list(references, indent + '    ')},\n"
        f"{indent}    tasks = listOf(\n{tasks},\n{indent}    ),\n"
        f"{indent})"
    )


def chapter_code(chapter: Json) -> str:
    sections = ",\n".join(section_code(section, "        ") for section in chapter["sections"])
    bubbles = chapter.get("bubbles")
    bubbles_line = (
        f"    bubbles = Bubbles({kotlin_string(bubbles[0])}, {kotlin_string(bubbles[1])}),\n" if bubbles else ""
    )
    return (
        "package dev.eftrainer.data.content\n\n"
        f"val File{chapter['id']} = Chapter(\n"
        f"    id = {kotlin_string(chapter['id'])},\n"
        f"    title = {kotlin_string(chapter['title'])},\n"
        f"{bubbles_line}"
        f"    sections = listOf(\n{sections},\n    ),\n"
        ")\n"
    )


def count_exercises(chapter: Json, *, generated: bool) -> int:
    return sum(
        len(task["exercises"])
        for section in chapter["sections"]
        for task in section["tasks"]
        if (task["origin"] == "GENERATED") == generated
    )


def main() -> None:
    chapter_id = sys.argv[1]
    chapter: Json = json.loads((ROOT / f"tools/content/{chapter_id}.json").read_text(encoding="utf-8"))
    report = validate(chapter)
    for warning in report.warnings:
        print("Предупреждение:", warning)
    if report.errors:
        sys.exit("Ошибки в JSON:\n  " + "\n  ".join(report.errors))
    out = OUT_DIR / f"File{chapter_id}.kt"
    out.write_text(chapter_code(chapter), encoding="utf-8", newline="\n")
    book_count = count_exercises(chapter, generated=False)
    preset_count = count_exercises(chapter, generated=True)
    print(f"{out.relative_to(ROOT)}: {len(chapter['sections'])} разд., {book_count} упр. + {preset_count} заготовок")


if __name__ == "__main__":
    main()
