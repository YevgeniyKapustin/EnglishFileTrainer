package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Reference
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Section
import dev.eftrainer.domain.check.filled

object GeneratePrompt {
    const val MAX_LEN = 160
    const val MAX_GAPS = 3
    const val SAMPLE_SIZE = 12

    private val SYSTEM =
        """
        You write practice exercises for a student of the textbook English File Pre-Intermediate (4th edition).
        Write NEW exercises on exactly the same grammar rule or vocabulary as the lesson section given below,
        in the same style and at the same level (A2-B1) as the examples from the book.
        Use only grammar and vocabulary from the lessons the student has studied. Do not copy the examples.
        Every exercise must have exactly one clearly correct answer (list real alternatives, e.g. contractions, in the answers).
        Answer with strict JSON only, no markdown, no comments, in this format:
        {"items":[
          {"type":"blank","instruction":"Complete the questions.","text":"Where {} born?","gaps":[["were you"]],"hint":"(you / be)"},
          {"type":"rewrite","instruction":"Put the words in the right order.","prompt":"live / where / you / do","answers":["Where do you live?"]}
        ]}
        "blank": text with one or more {} gaps, "gaps" has one list of accepted answers per {} (same count).
        "rewrite": the student writes the whole sentence; "answers" are all accepted full sentences.
        "hint" is optional. Keep every text shorter than 120 characters.
        """.trimIndent()

    fun build(
        chapter: Chapter,
        section: Section,
        studied: List<Chapter>,
        book: List<Exercise>,
        avoid: List<Exercise>,
        count: Int,
    ): List<Pair<String, String>> {
        val user =
            buildString {
                appendLine("Lesson ${chapter.id} \"${chapter.title}\", section: ${section.title}.")
                appendLine("Studied lessons: " + studied.joinToString("; ") { "${it.id} ${it.title}" } + ".")
                if (section.reference.isNotEmpty()) {
                    appendLine()
                    appendLine("Rules of this section:")
                    section.reference.forEach { appendLine(it.asText()) }
                }
                appendLine()
                appendLine("Examples from the book (with answers):")
                sample(book, SAMPLE_SIZE).forEach { appendLine("- " + it.asExample()) }
                if (avoid.isNotEmpty()) {
                    appendLine()
                    appendLine("Already used, do not repeat:")
                    avoid.takeLast(20).forEach { appendLine("- " + it.asExample()) }
                }
                appendLine()
                append("Write $count new exercises.")
            }
        return listOf("system" to SYSTEM, "user" to user)
    }

    fun <T> sample(
        items: List<T>,
        n: Int,
    ): List<T> = if (items.size <= n) items else List(n) { items[it * items.size / n] }

    private fun Exercise.asExample(): String =
        when (this) {
            is Rewrite -> {
                (prefill?.let { "correct if needed: $it" } ?: prompt) +
                    " -> " +
                    answers.first() +
                    (hint?.let { " $it" } ?: "")
            }

            is Blank -> {
                text.replace("{}", "___") + (hint?.let { " $it" } ?: "") + " -> " + filled()
            }

            else -> {
                toString()
            }
        }

    private fun Reference.asText(): String =
        when (this) {
            is Reference.Table -> {
                buildString {
                    appendLine("$title:")
                    rows.forEach { appendLine("  " + it.filter(String::isNotBlank).joinToString(" | ")) }
                }.trimEnd()
            }

            is Reference.Note -> {
                listOfNotNull(title, text).joinToString(": ")
            }

            is Reference.Mistake -> {
                """Typical mistake: "$wrong" -> "$right"""" + (why?.let { " ($it)" } ?: "")
            }
        }
}
