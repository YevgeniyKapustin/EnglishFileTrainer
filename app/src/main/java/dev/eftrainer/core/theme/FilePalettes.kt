package dev.eftrainer.core.theme

import androidx.compose.ui.graphics.Color

class FilePalette(
    val key: Color,
    val main: Color,
    val light: Color,
    val bright: Color,
    val soft: Color,
    val deep: Color,
    val darkest: Color,
    val accent: Color,
    val question: Color,
    val answer: Color,
) {
    val bubbleOverlap = Color(question.red * answer.red, question.green * answer.green, question.blue * answer.blue)
}

object FilePalettes {
    val Lagoon =
        FilePalette(
            key = Color(0xFF00ABC5),
            main = Color(0xFF00ADC2),
            light = Color(0xFF02A9C3),
            bright = Color(0xFF0099A6),
            soft = Color(0xFF0087A1),
            deep = Color(0xFF027F7C),
            darkest = Color(0xFF006460),
            accent = Color(0xFF70B135),
            question = Color(0xFF7DD5E1),
            answer = Color(0xFFB0D77A),
        )
    val Amber =
        FilePalette(
            key = Color(0xFFF7941D),
            main = Color(0xFFFAB812),
            light = Color(0xFFFDCC41),
            bright = Color(0xFFF4A711),
            soft = Color(0xFFEBB11D),
            deep = Color(0xFFE19706),
            darkest = Color(0xFFB67F17),
            accent = Color(0xFFEA5C3B),
            question = Color(0xFFFDDC89),
            answer = Color(0xFFF59582),
        )
    val Meadow =
        FilePalette(
            key = Color(0xFF6EC05C),
            main = Color(0xFF71BC5D),
            light = Color(0xFF82C36A),
            bright = Color(0xFF5CB24F),
            soft = Color(0xFF73B05B),
            deep = Color(0xFF47983A),
            darkest = Color(0xFF1C7327),
            accent = Color(0xFF009B83),
            question = Color(0xFFB5DEAC),
            answer = Color(0xFF5ACFC5),
        )
    val Coral =
        FilePalette(
            key = Color(0xFFF04D46),
            main = Color(0xFFEE4D45),
            light = Color(0xFFEC5067),
            bright = Color(0xFFE3443A),
            soft = Color(0xFFDC4C59),
            deep = Color(0xFFD7302A),
            darkest = Color(0xFF9E2833),
            accent = Color(0xFFED701B),
            question = Color(0xFFF5A6A2),
            answer = Color(0xFFF8A963),
        )

    private val cycle = listOf(Lagoon, Amber, Meadow, Coral)

    fun forFile(file: Int): FilePalette = cycle[(file - 1).mod(cycle.size)]

    fun forChapter(chapterId: String?): FilePalette = forFile(fileOf(chapterId))

    private fun fileOf(chapterId: String?): Int = chapterId?.takeWhile(Char::isDigit)?.toIntOrNull() ?: 1
}
