package dev.eftrainer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.domain.learning.Stats
import dev.eftrainer.domain.learning.Streak
import dev.eftrainer.domain.learning.WeakSpot
import dev.eftrainer.domain.learning.Week
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Month

data class StatsUi(
    val streak: Streak,
    val heat: List<List<Int?>>,
    val months: List<Month?>,
    val week: Week?,
    val weak: List<WeakSpot>,
    val completed: Int,
)

class StatsViewModel(
    private val container: AppContainer,
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
                        runCatching { Chapters.locate(id) }
                            .getOrNull()
                            ?.takeIf { it.chapter in Chapters.all }
                            ?.let { it.chapter to it.section }
                    },
                completed = done.size,
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE_MS), headerOnly())

    private fun headerOnly(): StatsUi? {
        val days = container.answers.latestDays ?: return null
        val completed = container.review.latest?.completed ?: return null
        val today = container.clock.today()
        return StatsUi(
            streak = Stats.streak(days, today),
            heat = Stats.heat(emptyList(), today),
            months = Stats.heatMonths(today),
            week = null,
            weak = emptyList(),
            completed = completed.size,
        )
    }

    private companion object {
        const val KEEP_ALIVE_MS = 5_000L
    }
}
