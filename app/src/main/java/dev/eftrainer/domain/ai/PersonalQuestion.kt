package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.domain.check.filled

object PersonalQuestion {
    private const val MIN_WORDS = 3
    private val ADDRESSED = Regex("\\b(you|your)\\b", RegexOption.IGNORE_CASE)
    private val SPEAKER = Regex("^[A-Z][A-Za-z]*:\\s*")
    private val QUOTE_MARKS = setOf('"', '“', '”', '‘')

    fun questionIn(exercise: Exercise): String? {
        if (exercise.id.startsWith(IrregularVerbs.ID_PREFIX)) return null
        val completed =
            when (exercise) {
                is Blank -> exercise.filled()
                is Rewrite -> exercise.answers.first()
                else -> return null
            }
        val question =
            completed
                .lines()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                ?.trimStart { !it.isLetterOrDigit() }
                ?.trim() ?: return null
        return question.takeIf { asksTheLearner(it) }
    }

    private fun asksTheLearner(line: String): Boolean =
        line.endsWith("?") &&
            !SPEAKER.containsMatchIn(line) &&
            ADDRESSED.containsMatchIn(line) &&
            line.split(' ').count { it.isNotBlank() } >= MIN_WORDS &&
            line.none { it in QUOTE_MARKS } &&
            !line.startsWith("'")
}
