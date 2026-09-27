package dev.eftrainer.ui.current

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.eftrainer.R
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.domain.learning.DayStep

@Composable
internal fun ReviewBar(
    state: CurrentUi,
    onReview: () -> Unit,
) {
    BottomBar {
        PrimaryButton(
            dayAction(state.step),
            onClick = onReview,
            enabled = state.step != DayStep.Rest,
        )
    }
}

@Composable
private fun dayAction(step: DayStep): String =
    when (step) {
        is DayStep.Continue -> stringResource(R.string.continue_chapter, step.chapter.id)
        is DayStep.Practice -> stringResource(R.string.practice_chapter, step.chapter.id)
        DayStep.Review, DayStep.Rest -> stringResource(R.string.review)
    }
