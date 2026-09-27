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

    data class Word(
        val text: String,
        override val spaceAfter: Boolean,
    ) : Token

    data class Gap(
        val index: Int,
        override val spaceAfter: Boolean,
    ) : Token
}

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
    modifier: Modifier = Modifier,
) {
    val tokens = tokenize(exercise.text)
    FlowRow(modifier, itemVerticalAlignment = Alignment.Bottom) {
        tokens.forEach { t ->
            val end = if (t.spaceAfter) 7.dp else 0.dp
            when (t) {
                is Token.Word -> {
                    Text(t.text, style = T.question, modifier = Modifier.padding(end = end))
                }

                is Token.Gap -> {
                    GapField(
                        index = t.index,
                        exercise = exercise,
                        values = values,
                        onChange = onChange,
                        checked = checked,
                        wrongGaps = wrongGaps,
                        focus = focus,
                        onAction = onAction,
                        modifier = Modifier.padding(end = end),
                    )
                }
            }
        }
        exercise.hint?.let {
            Text(it, style = T.hint, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        }
    }
}

@Composable
private fun GapField(
    index: Int,
    exercise: Blank,
    values: List<TextFieldValue>,
    onChange: (Int, TextFieldValue) -> Unit,
    checked: Boolean,
    wrongGaps: Set<Int>,
    focus: List<FocusRequester>,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val last = index == exercise.gaps.lastIndex
    val longest = exercise.gaps[index].maxOf { it.length }
    val color = fieldColor(checked, index in wrongGaps)
    val line = if (checked) color else C.Accent
    BasicTextField(
        value = values[index],
        onValueChange = { if (!checked) onChange(index, it) },
        readOnly = checked,
        singleLine = true,
        textStyle = T.question.copy(color = color, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(C.Accent),
        keyboardOptions = keyboard(if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = actions { if (last || checked) onAction() else focus[index + 1].requestFocus() },
        modifier =
            modifier
                .width((longest * 11 + 24).coerceIn(90, 260).dp)
                .focusRequester(focus[index])
                .drawBehind {
                    val y = size.height - 1.dp.toPx()
                    drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                },
        decorationBox = { inner -> Box(Modifier.padding(bottom = 2.dp)) { inner() } },
    )
}
