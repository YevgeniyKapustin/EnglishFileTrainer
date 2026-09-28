package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Task
import dev.eftrainer.domain.check.filled

object AcceptPrompt {
    private val ACCEPT = Regex(""""accept"\s*:\s*"?(true|false)"?""", RegexOption.IGNORE_CASE)

    private val SYSTEM =
        """
        You are a strict English teacher. Your student uses English File Pre-Intermediate (4th edition), level A2-B1.
        The student's answer to a homework exercise is not in the answer key. Decide if it is also a fully correct answer.
        accept = true only if ALL of these are true:
        - the grammar is correct and every word is spelled correctly;
        - it does exactly what the task asks and keeps every limit of the exercise: the words given in the exercise,
          in brackets, in the hint or in the word list, the form or tense the exercise practises (see the rules of the lesson),
          one answer per gap;
        - it means the same as the key answer and fits the rest of the exercise and the text around it.
        Only capital letters and the final full stop or question mark do not matter.
        accept = false if anything is wrong, missing or extra, if it uses other grammar than the exercise practises,
        if it changes the meaning, or if you are not completely sure.
        Answer with JSON only, no markdown, no other text:
        {"accept": false, "reason": "..."}
        """.trimIndent()

    fun asking(
        task: Task,
        exercise: Rewrite,
        answer: String,
        rules: String,
    ): List<Pair<String, String>> =
        prompt(task, rules) {
            appendLine("Exercise: ${exercise.prompt}")
            exercise.hint?.let { appendLine("Hint: $it") }
            appendLine("Key answers: " + exercise.answers.joinToString(" | "))
            append("Student's answer: ${answer.trim()}")
        }

    fun asking(
        task: Task,
        exercise: Blank,
        gaps: List<String>,
        rules: String,
    ): List<Pair<String, String>> =
        prompt(task, rules) {
            appendLine("Exercise (fill the gaps ___): ${exercise.text.replace("{}", "___")}")
            exercise.hint?.let { appendLine("Hint: $it") }
            appendLine("Key answer: ${exercise.filled()}")
            exercise.gaps.forEachIndexed { i, options ->
                appendLine("Key for gap ${i + 1}: " + options.joinToString(" | "))
            }
            gaps.forEachIndexed { i, gap -> appendLine("Student's gap ${i + 1}: ${gap.trim()}") }
            append("Student's answer: ${ExplainPrompt.answerText(exercise, gaps)}")
        }

    fun parseAccepted(raw: String): Boolean? =
        ACCEPT
            .find(raw)
            ?.groupValues
            ?.get(1)
            ?.lowercase()
            ?.toBooleanStrict()

    private fun prompt(
        task: Task,
        rules: String,
        exercise: StringBuilder.() -> Unit,
    ): List<Pair<String, String>> {
        val user =
            buildString {
                if (rules.isNotBlank()) {
                    appendLine("Rules of this lesson:")
                    appendLine(rules)
                    appendLine()
                }
                appendLine("Task: ${task.instruction}")
                task.context?.let { appendLine("Text: $it") }
                if (task.words.isNotEmpty()) appendLine("Word list: " + task.words.joinToString(", "))
                exercise()
            }
        return listOf("system" to SYSTEM, "user" to user)
    }
}
