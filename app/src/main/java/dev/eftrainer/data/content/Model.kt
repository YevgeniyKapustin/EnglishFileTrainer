package dev.eftrainer.data.content

sealed interface Exercise {
    val id: String
    val hint: String?
    val explain: String?
    val source: AnswerSource

    val clip: Clip? get() = null
}

data class Clip(
    val startMs: Int,
    val endMs: Int,
) {
    init {
        require(startMs >= 0 && endMs > startMs) { "клип $startMs..$endMs" }
    }
}

enum class AnswerSource {
    KEY,
    CONFIRMED,
    MODEL,
}

data class Rewrite(
    override val id: String,
    val prompt: String,
    val answers: List<String>,
    val prefill: String? = null,
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.KEY,
    override val clip: Clip? = null,
) : Exercise

data class Blank(
    override val id: String,
    val text: String,
    val gaps: List<List<String>>,
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.KEY,
    override val clip: Clip? = null,
) : Exercise {
    init {
        require(text.split("{}").size - 1 == gaps.size) { "$id: число {} не совпадает с gaps" }
    }
}

data class Speak(
    override val id: String,
    val text: String,
    val answers: List<String>,
    val audio: String? = null,
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.KEY,
    override val clip: Clip? = null,
) : Exercise

data class Talk(
    override val id: String,
    val situation: String,
    val goal: String,
    val opener: String,
    val criteria: List<String>,
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.CONFIRMED,
) : Exercise

data class Write(
    override val id: String,
    val prompt: String,
    val questions: List<String> = emptyList(),
    val criteria: List<String>,
    override val hint: String? = null,
    override val explain: String? = null,
    override val source: AnswerSource = AnswerSource.CONFIRMED,
) : Exercise

val Exercise.needsAi: Boolean get() = this is Talk || this is Write

val Exercise.optional: Boolean get() = needsAi

sealed interface Reference {
    data class Table(
        val title: String,
        val headers: List<String>,
        val rows: List<List<String>>,
        val audio: String? = null,
    ) : Reference

    data class Note(
        val title: String?,
        val text: String,
    ) : Reference

    data class Mistake(
        val wrong: String,
        val right: String,
        val why: String?,
    ) : Reference
}

data class Chapter(
    val id: String,
    val title: String,
    val sections: List<Section>,
    val bubbles: Bubbles? = null,
)

data class Bubbles(
    val first: String,
    val second: String,
)

enum class SectionKind { GRAMMAR, VOCABULARY, PRONUNCIATION, SKILLS, PRACTICAL_ENGLISH, REVIEW }

data class Section(
    val id: String,
    val kind: SectionKind,
    val title: String,
    val reference: List<Reference>,
    val tasks: List<Task>,
)

enum class Origin { WB, SB, SB_GRAMMAR_BANK, GENERATED }

data class Task(
    val id: String,
    val origin: Origin,
    val instruction: String,
    val exercises: List<Exercise>,
    val context: String? = null,
    val audio: String? = null,
    val video: String? = null,
)

val Task.number: String get() =
    if (origin == Origin.GENERATED) {
        id.substringAfterLast('-').removePrefix("x")
    } else {
        id
            .substringAfter('-')
            .removePrefix("sb")
            .removePrefix("wb")
            .removePrefix("gb")
            .removePrefix("pe")
    }

val Task.needsAi: Boolean get() = exercises.all { it.needsAi }

val Section.exercises: List<Exercise> get() =
    tasks.filter { it.origin != Origin.GENERATED }.flatMap { it.exercises }.filterNot { it.optional }

val Section.presets: List<Exercise> get() = tasks.filter { it.origin == Origin.GENERATED }.flatMap { it.exercises }

val Section.aiExercises: List<Exercise> get() = tasks.flatMap { it.exercises }.filter { it.needsAi }
val Chapter.exercises: List<Exercise> get() = sections.flatMap { it.exercises }
