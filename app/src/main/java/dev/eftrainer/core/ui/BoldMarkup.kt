package dev.eftrainer.core.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private const val BOLD_MARK = "**"

data class Marked(
    val plain: String,
    val bold: List<IntRange>,
)

fun parseBold(text: String): Marked {
    val parts = text.split(BOLD_MARK)
    if (parts.size % 2 == 0) return Marked(text, emptyList())
    val plain = StringBuilder()
    val bold = mutableListOf<IntRange>()
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1 && part.isNotEmpty()) bold += plain.length until plain.length + part.length
        plain.append(part)
    }
    return Marked(plain.toString(), bold)
}

fun boldMarkup(text: String): AnnotatedString {
    val marked = parseBold(text)
    return buildAnnotatedString {
        var from = 0
        for (range in marked.bold) {
            append(marked.plain.substring(from, range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(marked.plain.substring(range)) }
            from = range.last + 1
        }
        append(marked.plain.substring(from))
    }
}
