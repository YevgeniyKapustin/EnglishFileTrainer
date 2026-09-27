package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Blank

private sealed interface Token {
    val spaceAfter: Boolean
    data class Word(val text: String, override val spaceAfter: Boolean) : Token
    data class Gap(val index: Int, override val spaceAfter: Boolean) : Token
}

/** "Where {} born?" → слова и пропуски; пробел запоминаем, чтобы "{}?" не разъехалось. */
private fun tokenize(text: String): List<Token> {
    val raw = Regex("""\{\}|(?:(?!\{\})\S)+|\s+""").findAll(text).map { it.value }.toList()
    var gap = 0
    return raw.mapIndexedNotNull { i, s ->
        val spaceAfter = raw.getOrNull(i + 1)?.isBlank() == true
        when {
            s.isBlank() -> null
            s == "{}" -> Token.Gap(gap++, spaceAfter)
            else -> Token.Word(s, spaceAfter)
        }
    }
}

/** Пропуски прямо в тексте; ширина поля — по длине ответа, чтобы не подсказывать и не обрезать. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlankInput(
    exercise: Blank,
    values: List<TextFieldValue>,
    onChange: (Int, TextFieldValue) -> Unit,
    checked: Boolean,
    wrongGaps: Set<Int>,
    focus: List<FocusRequester>,
    onAction: () -> Unit,
) {
    val tokens = tokenize(exercise.text)
    FlowRow(itemVerticalAlignment = Alignment.Bottom) {
        tokens.forEach { t ->
            val end = if (t.spaceAfter) 7.dp else 0.dp
            when (t) {
                is Token.Word -> Text(t.text, style = T.question, modifier = Modifier.padding(end = end))
                is Token.Gap -> {
                    val i = t.index
                    val last = i == exercise.gaps.lastIndex
                    val longest = exercise.gaps[i].maxOf { it.length }
                    val color = fieldColor(checked, i in wrongGaps)
                    val line = if (checked) color else C.Accent
                    BasicTextField(
                        value = values[i],
                        onValueChange = { if (!checked) onChange(i, it) },
                        readOnly = checked,
                        singleLine = true,
                        textStyle = T.question.copy(color = color, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(C.Accent),
                        keyboardOptions = keyboard(if (last) ImeAction.Done else ImeAction.Next),
                        keyboardActions = actions { if (last || checked) onAction() else focus[i + 1].requestFocus() },
                        modifier = Modifier
                            .padding(end = end)
                            .width((longest * 11 + 24).coerceIn(90, 260).dp)
                            .focusRequester(focus[i])
                            .drawBehind {
                                val y = size.height - 1.dp.toPx()
                                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                            },
                        decorationBox = { inner -> Box(Modifier.padding(bottom = 2.dp)) { inner() } },
                    )
                }
            }
        }
        exercise.hint?.let {
            Text(it, style = T.hint, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        }
    }
}
