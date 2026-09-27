package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.TalkStep
import dev.eftrainer.ui.explain.LinkText

private val MarkerWidth = 24.dp

@Composable
internal fun TalkSteps(
    steps: List<TalkStep>,
    done: Set<Int>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val current = steps.indices.firstOrNull { it !in done }
    var hinted by rememberSaveable(current) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { i, step ->
            StepRow(i, step.text, finished = i in done, now = i == current && active)
            if (i == current && active) {
                if (hinted) {
                    Text(
                        step.example,
                        style = T.rowEn.copy(fontSize = 16.sp, lineHeight = 23.sp, fontStyle = FontStyle.Italic),
                        modifier = Modifier.padding(start = MarkerWidth),
                    )
                } else {
                    LinkText(
                        stringResource(R.string.talk_hint),
                        onClick = { hinted = true },
                        modifier = Modifier.padding(start = MarkerWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    text: String,
    finished: Boolean,
    now: Boolean,
) {
    val style =
        T.rowEn.copy(
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = if (now) C.Ink else C.Muted,
            fontWeight = if (now) FontWeight.SemiBold else FontWeight.Normal,
        )
    Row {
        Text(
            if (finished) "✓" else "${index + 1}",
            style = style.copy(color = if (finished) C.Ok else style.color),
            modifier = Modifier.width(MarkerWidth),
        )
        Text(text, style = style)
    }
}
