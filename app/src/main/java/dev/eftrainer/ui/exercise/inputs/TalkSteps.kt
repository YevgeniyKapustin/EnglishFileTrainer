package dev.eftrainer.ui.exercise.inputs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.TalkStep

@Composable
internal fun TalkStepPrompt(
    steps: List<TalkStep>,
    done: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val current = steps.indices.firstOrNull { it !in done } ?: return
    val step = steps[current]
    if (!step.asksQuestions) return
    Text(
        step.text,
        style = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp),
        modifier = modifier,
    )
}

private val TalkStep.asksQuestions: Boolean
    get() = text.startsWith("Ask ")
