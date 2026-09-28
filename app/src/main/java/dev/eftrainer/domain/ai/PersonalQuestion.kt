package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Task
import dev.eftrainer.domain.check.filled

object PersonalQuestion {
    private const val MIN_WORDS = 3
    private val ADDRESSED = Regex("\\b(you|your)\\b", RegexOption.IGNORE_CASE)
    private val ABOUT_SOMEONE_ELSE =
        Regex("\\b(i|me|my|mine|we|us|our|he|she|him|her|they|them|please)\\b", RegexOption.IGNORE_CASE)
    private val PAST_SCENE = Regex("[a-z,]\\s+when\\b", RegexOption.IGNORE_CASE)
    private val SPEAKER = Regex("^[A-Z][A-Za-z]*:\\s*")
    private val QUOTE_MARKS = setOf('"', '“', '”', '‘')
    private val STATEMENT_ENDS = setOf('.', '!')

    fun questionIn(
        exercise: Exercise,
        task: Task,
    ): String? {
        if (exercise.id.startsWith(IrregularVerbs.ID_PREFIX) || task.hasMoreThanTheQuestion(exercise)) return null
        val completed =
            when (exercise) {
                is Blank -> exercise.filled()
                is Rewrite -> exercise.answers.first()
                else -> return null
            }.trim()
        if (completed.lines().size > 1) return null
        val question = completed.trimStart { !it.isLetterOrDigit() }.trim()
        return question.takeIf { asksTheLearner(it) }
    }

    private fun Task.hasMoreThanTheQuestion(exercise: Exercise): Boolean =
        context != null ||
            dialogue ||
            video != null ||
            (audio != null && exercise.clip == null) ||
            images.isNotEmpty() ||
            exercise.image != null ||
            exercises.any { SPEAKER.containsMatchIn(it.shownText()) }

    private fun Exercise.shownText(): String =
        when (this) {
            is Blank -> text
            is Rewrite -> prompt
            else -> ""
        }.trim()

    private fun asksTheLearner(line: String): Boolean =
        line.endsWith("?") &&
            !SPEAKER.containsMatchIn(line) &&
            ADDRESSED.containsMatchIn(line) &&
            !ABOUT_SOMEONE_ELSE.containsMatchIn(line) &&
            !PAST_SCENE.containsMatchIn(line) &&
            line.substringBefore('?').none { it in STATEMENT_ENDS } &&
            line.split(' ').count { it.isNotBlank() } >= MIN_WORDS &&
            line.none { it in QUOTE_MARKS } &&
            !line.startsWith("'")
}
