package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Exercise
import java.time.LocalDate

data class DayRun(
    val key: String,
    val date: LocalDate,
    val order: List<String>,
    val run: TaskRun,
    val arrivals: Int,
) {
    fun resume(
        key: String,
        today: LocalDate,
        lookup: (String) -> Exercise?,
    ): ResumedSession? {
        if (key != this.key || date != today || order.isEmpty()) return null
        val exercises = order.map { lookup(it) ?: return null }
        val tally = SessionTally(exercises, run)
        return if (tally.finished) null else ResumedSession(tally, arrivals)
    }

    companion object {
        fun keyOf(step: DayStep): String =
            when (step) {
                DayStep.Review -> "review"
                is DayStep.Continue -> "continue-${step.chapter.id}"
                is DayStep.Practice -> "practice-${step.chapter.id}"
                DayStep.Rest -> "rest"
            }

        fun snapshot(
            key: String,
            today: LocalDate,
            tally: SessionTally,
            arrivals: Int,
        ): DayRun = DayRun(key, today, tally.order, tally.run, arrivals)
    }
}

data class ResumedSession(
    val tally: SessionTally,
    val arrivals: Int,
)
