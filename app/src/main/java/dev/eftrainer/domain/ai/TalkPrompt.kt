package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Talk

object TalkPrompt {
    const val MAX_TURNS = 8

    private const val END = "[END]"

    private const val MIN_TURNS_TO_END = 2
    private const val UNICODE_ESCAPE_LENGTH = 6
    private const val HEX_RADIX = 16
    private val PASS = Regex(""""pass"\s*:\s*"?(true|false)"?""", RegexOption.IGNORE_CASE)
    private val SUMMARY = Regex(""""summary"\s*:\s*"((?:[^"\\]|\\.)*)"""", RegexOption.DOT_MATCHES_ALL)

    data class Grade(
        val pass: Boolean,
        val summary: String,
    )

    fun conversation(
        talk: Talk,
        lines: List<ChatLine>,
    ): List<Pair<String, String>> {
        val system =
            """
            You are role-playing a short conversation with a student of English (level Pre-Intermediate, A2-B1).
            Situation and your role: ${talk.situation}
            The student's task: ${talk.goal}
            Rules:
            - Stay in your role all the time. Never say you are an AI or a teacher.
            - Use simple Pre-Intermediate English. 1-2 short sentences per turn.
            - Keep the conversation going: react to what the student says and often ask a question back.
            - Never correct the student's mistakes and never comment on their English. Just continue the conversation.
            - No markdown, no emojis, no lists. Plain text only. Do not write your name before the line.
            - When the student has done the task and the conversation has come to a natural end, say a short goodbye and add $END at the very end.
            """.trimIndent()
        return listOf("system" to system, "user" to "(Start the conversation.)") +
            lines.map { (if (it.student) "user" else "assistant") to it.text }
    }

    data class Reply(
        val text: String,
        val ended: Boolean,
    )

    fun reply(
        raw: String,
        studentTurns: Int,
    ): Reply {
        val ended = raw.contains(END, ignoreCase = true)
        val text =
            raw
                .replace(END, "", ignoreCase = true)
                .replace("*", "")
                .trim()
                .removeSurrounding("\"")
                .replace(Regex("^(Partner|You|AI|Assistant)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
        return Reply(text, ended && studentTurns >= MIN_TURNS_TO_END)
    }

    fun grading(
        talk: Talk,
        rules: String,
        lines: List<ChatLine>,
    ): List<Pair<String, String>> {
        val system =
            """
            You are a friendly English teacher. Your student uses English File Pre-Intermediate (4th edition).
            The student had a short conversation with a partner. Grade only the student's lines.
            pass = true if the student did the task and meets the criteria. Small mistakes are OK;
            pass = false if the task is not done or there are many mistakes in the grammar of this lesson.
            summary = 1-3 short sentences in simple English, addressed to the student ("You ..."):
            what was good, and the main mistakes with the grammar of this lesson, with the correct form.
            Answer with JSON only, no markdown, no other text: {"pass": true, "summary": "..."}
            """.trimIndent()
        val user =
            buildString {
                appendLine("Situation: ${talk.situation}")
                appendLine("Student's task: ${talk.goal}")
                appendLine("Criteria:")
                talk.criteria.forEach { appendLine("- $it") }
                if (rules.isNotBlank()) {
                    appendLine()
                    appendLine("Rules of this lesson:")
                    appendLine(rules)
                }
                appendLine()
                appendLine("Conversation:")
                lines.forEach { appendLine((if (it.student) "Student: " else "Partner: ") + it.text) }
            }
        return listOf("system" to system, "user" to user.trimEnd())
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
                ?.let(::unescape)
                ?.trim()
        if (summary.isNullOrEmpty()) return null
        return Grade(pass, summary)
    }

    internal fun unescape(s: String): String =
        buildString {
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c != '\\' || i == s.lastIndex) {
                    append(c)
                    i++
                    continue
                }
                when (val n = s[i + 1]) {
                    'n' -> {
                        append('\n')
                    }

                    't' -> {
                        append('\t')
                    }

                    'r' -> {}

                    'u' -> {
                        if (i + UNICODE_ESCAPE_LENGTH <= s.length) {
                            s
                                .substring(
                                    i + 2,
                                    i + UNICODE_ESCAPE_LENGTH,
                                ).toIntOrNull(HEX_RADIX)
                                ?.let { append(it.toChar()) }
                            i += UNICODE_ESCAPE_LENGTH - 2
                        }
                    }

                    else -> {
                        append(n)
                    }
                }
                i += 2
            }
        }
}
