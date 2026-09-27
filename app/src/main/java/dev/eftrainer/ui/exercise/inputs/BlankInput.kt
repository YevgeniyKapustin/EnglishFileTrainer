package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Blank

internal sealed interface Token {
    val spaceAfter: Boolean

    data class Word(
        val text: String,
        override val spaceAfter: Boolean,
    ) : Token

    data class Gap(
        val index: Int,
        override val spaceAfter: Boolean,
    ) : Token

    data object LineBreak : Token {
        override val spaceAfter = false
    }
}

internal fun tokenize(text: String): List<Token> {
    val raw = Regex("""\{\}|(?:(?!\{\})\S)+|\s+""").findAll(text).map { it.value }.toList()
    var gap = 0
    return raw.mapIndexedNotNull { i, s ->
        val spaceAfter = raw.getOrNull(i + 1)?.isBlank() == true
        when {
            s.isBlank() -> if ('\n' in s) Token.LineBreak else null
            s == "{}" -> Token.Gap(gap++, spaceAfter)
            else -> Token.Word(s, spaceAfter)
        }
    }
}

@Stable
class GapState(
    val exercise: Blank,
    val values: List<TextFieldValue>,
    val onChange: (Int, TextFieldValue) -> Unit,
    val checked: Boolean,
    val wrongGaps: Set<Int>,
    val focus: List<FocusRequester>,
    val onAction: () -> Unit,
)

@Composable
fun BlankInput(
    gaps: GapState,
    modifier: Modifier = Modifier,
) {
    GapFlow(tokenize(gaps.exercise.text), gaps, T.question, gaps.exercise.hint, modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GapFlow(
    tokens: List<Token>,
    gaps: GapState,
    style: TextStyle,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, itemVerticalAlignment = Alignment.Bottom) {
        tokens.forEach { t ->
            val end = if (t.spaceAfter) 7.dp else 0.dp
            when (t) {
                is Token.Word -> {
                    Text(t.text, style = style, modifier = Modifier.padding(end = end))
                }

                Token.LineBreak -> {
                    Spacer(Modifier.fillMaxWidth())
                }

                is Token.Gap -> {
                    GapField(t.index, gaps, style, Modifier.padding(end = end))
                }
            }
        }
        hint?.let {
            Text(
                it,
                style = T.hint.copy(fontSize = style.fontSize * HINT_SCALE),
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
    }
}

private const val HINT_SCALE = 0.78f

@Composable
private fun GapField(
    index: Int,
    gaps: GapState,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val exercise = gaps.exercise
    val checked = gaps.checked
    val focus = gaps.focus
    val onAction = gaps.onAction
    val last = index == exercise.gaps.lastIndex
    val longest = exercise.gaps[index].maxOf { it.length }
    val color = fieldColor(checked, index in gaps.wrongGaps)
    val line = if (checked) color else C.Accent
    val charWidth = style.fontSize.value / 2
    BasicTextField(
        value = gaps.values[index],
        onValueChange = { if (!checked) gaps.onChange(index, it) },
        readOnly = checked,
        singleLine = true,
        textStyle = style.copy(color = color, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(C.Accent),
        keyboardOptions = keyboard(if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = actions { if (last || checked) onAction() else focus[index + 1].requestFocus() },
        modifier =
            modifier
                .width((longest * charWidth + 24).coerceIn(90f, 260f).dp)
                .focusRequester(focus[index])
                .drawBehind {
                    val y = size.height - 1.dp.toPx()
                    drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                },
        decorationBox = { inner -> Box(Modifier.padding(bottom = 2.dp)) { inner() } },
    )
}
