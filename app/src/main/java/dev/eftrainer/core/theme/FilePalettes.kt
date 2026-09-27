package dev.eftrainer.core.theme

import androidx.compose.ui.graphics.Color

class FilePalette(
    val key: Color,
    val accent: Color,
    val light: Color,
    val main: Color,
    val mid: Color,
    val deep: Color,
    val teal: Color,
    val flap: Color,
    val dark: Color,
    val question: Color,
    val answer: Color,
) {
    val bubbleOverlap = Color(question.red * answer.red, question.green * answer.green, question.blue * answer.blue)
}

object FilePalettes {
    val Lagoon =
        FilePalette(
            key = Color(0xFF00ABC5),
            accent = Color(0xFF73B237),
            light = Color(0xFF019AA9),
            main = Color(0xFF00ABC4),
            mid = Color(0xFF008297),
            deep = Color(0xFF027A93),
            teal = Color(0xFF017F7F),
            flap = Color(0xFF008196),
            dark = Color(0xFF01615E),
            question = Color(0xFF7DD5E1),
            answer = Color(0xFFB0D77A),
        )
    val Amber =
        FilePalette(
            key = Color(0xFFF7941D),
            accent = Color(0xFFE65D39),
            light = Color(0xFFF3A811),
            main = Color(0xFFFCB913),
            mid = Color(0xFFE6B221),
            deep = Color(0xFFFFCB41),
            teal = Color(0xFFDF9109),
            flap = Color(0xFFD2A530),
            dark = Color(0xFFBA8118),
            question = Color(0xFFFDDC89),
            answer = Color(0xFFF59582),
        )
    val Meadow =
        FilePalette(
            key = Color(0xFF6EC05C),
            accent = Color(0xFF019B7F),
            light = Color(0xFF60B152),
            main = Color(0xFF6EC05C),
            mid = Color(0xFF72B05A),
            deep = Color(0xFF85CA6D),
            teal = Color(0xFF4B973C),
            flap = Color(0xFF239041),
            dark = Color(0xFF177328),
            question = Color(0xFFB5DEAC),
            answer = Color(0xFF5ACFC5),
        )
    val Coral =
        FilePalette(
            key = Color(0xFFF04D46),
            accent = Color(0xFFEE701C),
            light = Color(0xFFE8433D),
            main = Color(0xFFF04D46),
            mid = Color(0xFFDB4D59),
            deep = Color(0xFFF24D6B),
            teal = Color(0xFFDB2F2D),
            flap = Color(0xFFAF454F),
            dark = Color(0xFF9E2A33),
            question = Color(0xFFF5A6A2),
            answer = Color(0xFFF8A963),
        )

    private val cycle = listOf(Lagoon, Amber, Meadow, Coral)

    fun forFile(file: Int): FilePalette = cycle[(file - 1).mod(cycle.size)]

    fun forChapter(chapterId: String?): FilePalette = forFile(fileOf(chapterId))

    private fun fileOf(chapterId: String?): Int = chapterId?.takeWhile(Char::isDigit)?.toIntOrNull() ?: 1
}
