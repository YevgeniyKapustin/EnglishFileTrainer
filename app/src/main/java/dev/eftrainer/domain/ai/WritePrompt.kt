package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Write

object WritePrompt {
    const val MAX_CORRECTIONS = 6
    private const val STRING = """"((?:[^"\\]|\\.)*)""""
    private val PASS = Regex(""""pass"\s*:\s*"?(true|false)"?""", RegexOption.IGNORE_CASE)
    private val SUMMARY = Regex(""""summary"\s*:\s*$STRING""", RegexOption.DOT_MATCHES_ALL)
    private val CORRECTIONS = Regex(""""corrections"\s*:\s*\[""")
    private val OBJECT = Regex("\\{[^{}]*\\}")
    private val MISTAKE = Regex(""""mistake"\s*:\s*$STRING""", RegexOption.DOT_MATCHES_ALL)
    private val CORRECTION = Regex(""""correction"\s*:\s*$STRING""", RegexOption.DOT_MATCHES_ALL)

    data class Correction(
        val mistake: String,
        val correction: String,
    )

    data class Grade(
        val pass: Boolean,
        val summary: String,
        val corrections: List<Correction>,
    )

    fun grading(
        write: Write,
        instruction: String,
        rules: String,
        answer: String,
    ): List<Pair<String, String>> {
        val system =
            """
            You are a friendly English teacher. Your student uses English File Pre-Intermediate (4th edition), level A2-B1.
            The student wrote an answer to a homework task. Check only the English, not the facts: any personal content is fine.
            pass = true if the student did the task and meets the criteria. Small mistakes are OK;
            pass = false if the task is not done or there are many mistakes in the grammar of this lesson.
            summary = 1-2 short sentences in simple English, addressed to the student ("You ..."): what was good and what to work on.
            corrections = the real mistakes in the student's text (grammar, word order, verb forms, spelling), at most $MAX_CORRECTIONS,
            the most important first. "mistake" = the wrong words exactly as the student wrote them (a short part of the sentence);
            "correction" = the same part, corrected, in simple Pre-Intermediate English. Do not rewrite correct sentences
            to make them sound better. No mistakes -> an empty list.
            Answer with JSON only, no markdown, no other text:
            {"pass": true, "summary": "...", "corrections": [{"mistake": "...", "correction": "..."}]}
            """.trimIndent()
        val user =
            buildString {
                appendLine("Task: $instruction")
                appendLine("What to write: ${write.prompt}")
                if (write.questions.isNotEmpty()) {
                    appendLine("Questions:")
                    write.questions.forEachIndexed { i, q -> appendLine("${i + 1} $q") }
                }
                write.scene?.let {
                    appendLine("The student listened to a recording. You cannot hear it; this is what happens in it:")
                    appendLine(it)
                    appendLine(
                        "So here also check that the text fits what happens in the recording; " +
                            "any sensible way to describe it is fine.",
                    )
                }
                appendLine("Criteria:")
                write.criteria.forEach { appendLine("- $it") }
                if (rules.isNotBlank()) {
                    appendLine()
                    appendLine("Rules of this lesson:")
                    appendLine(rules)
                }
                appendLine()
                appendLine("Student's text:")
                append(answer.trim())
            }
        return listOf("system" to system, "user" to user)
    }

    fun parseGrade(raw: String): Grade? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val json = raw.substring(start, end + 1)
        val pass =
            PASS
                .find(json)
                ?.groupValues
                ?.get(1)
                ?.lowercase()
                ?.toBooleanStrict() ?: return null
        val summary =
            SUMMARY
                .find(json)
                ?.groupValues
                ?.get(1)
                ?.let(TalkPrompt::unescape)
                ?.trim()
        if (summary.isNullOrEmpty()) return null
        val corrections =
            CORRECTIONS
                .find(json)
                ?.let { m ->
                    OBJECT
                        .findAll(json.substring(m.range.last + 1))
                        .mapNotNull { o ->
                            val mistake =
                                MISTAKE
                                    .find(o.value)
                                    ?.groupValues
                                    ?.get(1)
                                    ?.let(TalkPrompt::unescape)
                                    ?.trim()
                            val correction =
                                CORRECTION
                                    .find(o.value)
                                    ?.groupValues
                                    ?.get(1)
                                    ?.let(TalkPrompt::unescape)
                                    ?.trim()
                            if (mistake.isNullOrEmpty() || correction.isNullOrEmpty() || mistake == correction) {
                                null
                            } else {
                                Correction(mistake, correction)
                            }
                        }.take(MAX_CORRECTIONS)
                        .toList()
                }.orEmpty()
        return Grade(pass, summary, corrections)
    }
}
