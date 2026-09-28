package dev.eftrainer.ui.exercise.inputs

import dev.eftrainer.core.ui.parseBold

object QuestionSpeech {
    private const val GAP_PAUSE = "…"
    private val sentenceEnd = Regex("""[.!?…:;]["'’”)]*$""")

    fun text(text: String): String = lines(text.lines())

    fun lines(lines: List<String>): String =
        lines
            .map { parseBold(it).plain.replace("{}", GAP_PAUSE).trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ") { if (sentenceEnd.containsMatchIn(it)) it else "$it." }
}
