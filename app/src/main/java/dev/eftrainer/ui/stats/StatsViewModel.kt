package dev.eftrainer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.domain.learning.Stats
import dev.eftrainer.domain.learning.Streak
import dev.eftrainer.domain.learning.WeakSpot
import dev.eftrainer.domain.learning.Week
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Month

data class StatsUi(
    val streak: Streak,
    val heat: List<List<Int?>>,
    val months: List<Month?>,
    val week: Week,
    val weak: List<WeakSpot>,
    val completed: Int,
)

class StatsViewModel(
    container: AppContainer,
) : ViewModel() {
    val state: StateFlow<StatsUi?> =
        combine(
            container.answers.answers,
            container.answers.days,
            container.chapters.completed,
        ) { answers, days, done ->
            val today = container.clock.today()
            StatsUi(
                streak = Stats.streak(days, today),
                heat = Stats.heat(answers, today),
                months = Stats.heatMonths(today),
                week = Stats.week(answers, today),
                weak =
                    Stats.weakSpots(answers, today) { id ->
                        runCatching { Chapters.locate(id) }.getOrNull()?.let { it.chapter to it.section }
                    },
                completed = done.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
