package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Reference
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.Speak
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Task
import dev.eftrainer.data.content.Write

object ExplainPrompt {
    private val SYSTEM =
        """
        You are a friendly English teacher. Your student uses the textbook English File Pre-Intermediate (4th edition).
        Explain why the student's answer is wrong and what the correct rule is.
        Use simple English (level A2-B1), 2-4 short sentences. Refer to the rules of the lesson given below.
        Do not use grammar the student has not studied yet. No markdown, no lists, plain text only.
        If the student asks a follow-up question, answer it the same way: short and simple.
        """.trimIndent()

    private val SELECTION_SYSTEM =
        """
        You are a friendly English teacher. Your student uses the textbook English File Pre-Intermediate (4th edition).
        The student selected a piece of text in the app and wants you to explain it: what it means and the grammar
        or vocabulary behind it, in the context given below.
        Use simple English (level A2-B1), 2-5 short sentences. Refer to the rules of the lesson given below.
        Only use grammar from the lessons the student has studied (listed below), do not go ahead.
        No markdown, no lists, plain text only.
        If the student asks a follow-up question, answer it the same way: short and simple.
        """.trimIndent()

    fun build(
        chapter: Chapter,
        section: Section,
        task: Task,
        exercise: Exercise,
        answer: String,
        expected: String,
        rules: List<Section> = listOf(section),
    ): List<Pair<String, String>> {
        val user =
            buildString {
                appendLine("Lesson ${chapter.id} \"${chapter.title}\", section: ${section.title}.")
                if (rules.any { it.reference.isNotEmpty() }) {
                    appendLine()
                    appendLine("Rules of this lesson:")
                    appendLine(rules(rules))
                }
                appendLine()
                appendLine("Task: ${task.instruction}")
                task.context?.let { appendLine("Text: $it") }
                appendLine("Exercise: ${exercise.asText()}")
                exercise.hint?.let { appendLine("Hint: $it") }
                appendLine("Student's answer: $answer")
                append("Correct answer: $expected")
            }
        return listOf("system" to SYSTEM, "user" to user)
    }

    data class Context(
        val chapter: Chapter,
        val section: Section,
        val studied: List<Chapter>,
        val task: Task? = null,
        val exercise: Exercise? = null,
        val answer: String? = null,
        val expected: String? = null,
    )

    fun selection(
        text: String,
        ctx: Context,
    ): List<Pair<String, String>> {
        val user =
            buildString {
                appendLine(
                    "Lessons studied so far: " + ctx.studied.joinToString(", ") { "${it.id} \"${it.title}\"" } + ".",
                )
                appendLine("Now: lesson ${ctx.chapter.id} \"${ctx.chapter.title}\", section: ${ctx.section.title}.")
                if (ctx.section.reference.isNotEmpty()) {
                    appendLine()
                    appendLine("Rules of this section:")
                    appendLine(rules(listOf(ctx.section)))
                }
                ctx.task?.let { task ->
                    appendLine()
                    appendLine("Task: ${task.instruction}")
                    task.context?.let { appendLine("Text: $it") }
                }
                ctx.exercise?.let { e ->
                    appendLine("Exercise: ${e.asText()}")
                    e.hint?.let { appendLine("Hint: $it") }
                }
                ctx.answer?.let { appendLine("Student's answer: $it") }
                ctx.expected?.let { appendLine("Correct answer: $it") }
                appendLine()
                append("Explain this: \"${text.trim()}\"")
            }
        return listOf("system" to SELECTION_SYSTEM, "user" to user)
    }

    fun rules(sections: List<Section>): String = sections.flatMap { it.reference }.joinToString("\n") { it.asText() }

    fun answerText(
        exercise: Exercise,
        inputs: List<String>,
    ): String =
        when (exercise) {
            is Rewrite, is Speak, is Write -> {
                inputs.single().trim()
            }

            is Talk -> {
                inputs.joinToString(" / ")
            }

            is Blank -> {
                exercise.text.split("{}").let { parts ->
                    parts.mapIndexed { i, p -> if (i < inputs.size) p + inputs[i].trim() else p }.joinToString("")
                }
            }
        }

    private fun Exercise.asText(): String =
        when (this) {
            is Rewrite -> {
                prefill?.let { "correct the sentence if needed: $it" } ?: prompt
            }

            is Blank -> {
                text.replace("{}", "___")
            }

            is Speak -> {
                "say aloud: $text"
            }

            is Talk -> {
                "conversation. $goal"
            }

            is Write -> {
                "written answer. $prompt" +
                    questions.joinToString(
                        " ",
                        prefix = if (questions.isEmpty()) "" else " Questions: ",
                    )
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
