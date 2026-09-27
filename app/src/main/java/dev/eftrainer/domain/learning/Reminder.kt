package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.db.entities.Progress
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

sealed interface Nudge {
    data class Review(
        val count: Int,
    ) : Nudge

    data class Continue(
        val chapter: Chapter,
    ) : Nudge
}

object Reminder {
    fun nudge(
        progress: Map<String, Progress>,
        plan: ReviewPlan,
        current: Chapter?,
        today: LocalDate,
    ): Nudge? =
        when {
            progress.values.any { it.lastAnsweredOn == today } -> null
            plan.all.isNotEmpty() -> Nudge.Review(plan.all.size)
            current != null -> Nudge.Continue(current)
            else -> null
        }

    fun delayUntil(
        now: LocalDateTime,
        at: LocalTime,
    ): Duration {
        val todayAt = now.toLocalDate().atTime(at)
        val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        return Duration.between(now, next)
    }
}
