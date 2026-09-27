package dev.eftrainer.ui.exercise.inputs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Sort
import dev.eftrainer.data.content.Stress

@Stable
class TapAnswer(
    exercise: Exercise,
) {
    private val minPicks: Int? =
        when (exercise) {
            is Stress -> if (exercise.syllables) 1 else 0
            is Sort -> 1
            else -> null
        }

    var order by mutableStateOf((exercise as? Order)?.pictures?.map { it.label }.orEmpty())
    var picks by mutableStateOf(emptyList<Int>())

    val filled: Boolean get() = picks.size >= (minPicks ?: 0)

    fun texts(): List<String>? =
        when {
            minPicks != null -> picks.map { it.toString() }
            order.isNotEmpty() -> order
            else -> null
        }
}

@Composable
fun TapInput(
    exercise: Exercise,
    answer: TapAnswer,
    checked: Boolean,
    wrong: Set<Int>,
) {
    when (exercise) {
        is Order -> OrderInput(exercise, answer.order, { answer.order = it }, checked, wrong)
        is Stress -> StressInput(exercise, answer.picks, { answer.picks = it }, checked, wrong)
        is Sort -> SortInput(exercise, answer.picks.firstOrNull(), { answer.picks = listOf(it) }, checked)
        else -> Unit
    }
}
