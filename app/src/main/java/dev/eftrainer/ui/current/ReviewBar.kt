package dev.eftrainer.ui.current

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.domain.learning.DayStep

@Composable
internal fun ReviewBar(
    state: CurrentUi,
    onReview: () -> Unit,
) {
    BottomBar {
        Text(
            dayLine(state),
            style = T.label.copy(fontSize = 15.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
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

@Composable
private fun dayLine(state: CurrentUi): String =
    when (val step = state.step) {
        is DayStep.Continue -> pluralStringResource(R.plurals.left_in_chapter, state.left, state.left, step.chapter.id)
        is DayStep.Practice -> stringResource(R.string.practice_until_tomorrow, step.chapter.id)
        DayStep.Review, DayStep.Rest -> reviewLine(state)
    }

@Composable
private fun reviewLine(state: CurrentUi): String {
    val sections = state.reviewSections.joinToString(" and ")
    return when {
        state.reviewCurrent > 0 && state.reviewDue > 0 -> {
            pluralStringResource(R.plurals.exercises_from, state.reviewCurrent, state.reviewCurrent, sections) +
                " " +
                pluralStringResource(R.plurals.and_from_earlier, state.reviewDue, state.reviewDue)
        }

        state.reviewCurrent > 0 -> {
            pluralStringResource(
                R.plurals.exercises_from,
                state.reviewCurrent,
                state.reviewCurrent,
                sections,
            )
        }

        state.reviewDue > 0 -> {
            pluralStringResource(R.plurals.exercises_from_earlier, state.reviewDue, state.reviewDue)
        }

        else -> {
            stringResource(R.string.nothing_to_review)
        }
    }
}
