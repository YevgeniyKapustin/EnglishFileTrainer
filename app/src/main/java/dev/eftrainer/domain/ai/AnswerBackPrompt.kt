package dev.eftrainer.domain.ai

object AnswerBackPrompt {
    fun grading(
        question: String,
        answer: String,
    ): List<Pair<String, String>> {
        val system =
            """
            You are a friendly English teacher. Your student uses English File Pre-Intermediate (4th edition), level A2-B1.
            The student was asked a question about themselves and answered it in English. Check only the English and
            whether the answer fits the question, not the facts: any personal content is fine, a short answer is fine.
            pass = true if the answer really answers the question and the English is correct for this level. Small slips are OK;
            pass = false if it does not answer the question, or there are mistakes in grammar, word order or verb forms.
            summary = 1 short sentence in simple English, addressed to the student ("You ...").
            corrections = the real mistakes in the student's answer (grammar, word order, verb forms, spelling),
            at most ${WritePrompt.MAX_CORRECTIONS}, the most important first. "mistake" = the wrong words exactly as the student
            wrote them; "correction" = the same part, corrected, in simple Pre-Intermediate English. Do not rewrite correct
            answers to make them sound better. No mistakes -> an empty list.
            Answer with JSON only, no markdown, no other text:
            {"pass": true, "summary": "...", "corrections": [{"mistake": "...", "correction": "..."}]}
            """.trimIndent()
        val user =
            buildString {
                appendLine("Question: $question")
                append("Student's answer: ${answer.trim()}")
            }
        return listOf("system" to system, "user" to user)
    }

    fun parseGrade(raw: String): WritePrompt.Grade? = WritePrompt.parseGrade(raw)
}
