package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Sort
import dev.eftrainer.ui.appContainer

private val ColumnShape = RoundedCornerShape(12.dp)

@Composable
fun SortInput(
    exercise: Sort,
    pick: Int?,
    onPick: (Int) -> Unit,
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val sounds = appContainer().sounds
    val word = markedWord(exercise.word)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            word,
            style = T.question.copy(fontSize = 34.sp, lineHeight = 44.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            exercise.columns.forEachIndexed { i, title ->
                val border =
                    when {
                        checked && i == exercise.answer -> C.Ok
                        checked && i == pick -> C.Bad
                        i == pick -> C.Accent
                        else -> C.Line
                    }
                SortColumn(
                    title = title,
                    word = word.takeIf { i == pick || (checked && i == exercise.answer) },
                    border = border,
                    enabled = !checked,
                    onClick = {
                        onPick(i)
                        sounds.play(Cue.TICK)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SortColumn(
    title: String,
    word: AnnotatedString?,
    border: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(ColumnShape)
            .background(C.Field)
            .border(if (border == C.Line) 1.dp else 1.5.dp, border, ColumnShape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            title,
            style = T.label.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
        )
        Box(
            Modifier.fillMaxWidth().height(72.dp).background(C.Line.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            word?.let {
                Text(
                    it,
                    style = T.rowEn.copy(fontSize = 18.sp, color = border.takeUnless { c -> c == C.Line } ?: C.Ink),
                )
            }
        }
    }
}

private fun markedWord(word: String): AnnotatedString =
    buildAnnotatedString {
        var pink = false
        word.forEach { ch ->
            when (ch) {
                '[' -> pink = true
                ']' -> pink = false
                '|' -> withStyle(SpanStyle(color = C.Chevron, fontWeight = FontWeight.Normal)) { append("|") }
                else -> if (pink) withStyle(SpanStyle(color = EF.P)) { append(ch) } else append(ch)
            }
        }
    }
