package dev.eftrainer.ui.exercise

import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.settings.DayRuns
import dev.eftrainer.data.settings.TaskRuns
import dev.eftrainer.domain.learning.DayRun
import dev.eftrainer.domain.learning.DayStep
import dev.eftrainer.domain.learning.ResumedSession
import dev.eftrainer.domain.learning.SessionTally
import java.time.LocalDate

sealed interface SessionRun {
    fun start(exercises: List<Exercise>): ResumedSession

    fun save(
        tally: SessionTally,
        arrivals: Int,
    )

    fun clear()

    companion object {
        fun resolve(
            container: AppContainer,
            dayStep: DayStep?,
            generated: Map<String, List<Exercise>>,
            pickedTaskId: String?,
        ): SessionRun =
            dayStep?.let { step ->
                ForDay(
                    container.dayRuns,
                    DayRun.keyOf(step),
                    container.clock.today(),
                    generated.values.flatten().associateBy(Exercise::id),
                )
            } ?: ForTask(container.taskRuns, pickedTaskId)
    }

    class ForTask(
        private val runs: TaskRuns,
        private val taskId: String?,
    ) : SessionRun {
        override fun start(exercises: List<Exercise>): ResumedSession =
            ResumedSession(taskId?.let { SessionTally.resume(exercises, runs.load(it)) } ?: SessionTally(exercises), 0)

        override fun save(
            tally: SessionTally,
            arrivals: Int,
        ) {
            taskId?.let { runs.save(it, tally.run) }
        }

        override fun clear() {
            taskId?.let(runs::clear)
        }
    }

    class ForDay(
        private val runs: DayRuns,
        private val key: String,
        private val today: LocalDate,
        private val generated: Map<String, Exercise>,
    ) : SessionRun {
        override fun start(exercises: List<Exercise>): ResumedSession {
            val resumed = runs.load()?.resume(key, today) { generated[it] ?: Chapters.exerciseOrNull(it) }
            if (resumed != null) return resumed
            runs.clear()
            val fresh = ResumedSession(SessionTally(exercises), 0)
            if (!fresh.tally.finished) save(fresh.tally, 0)
            return fresh
        }

        override fun save(
            tally: SessionTally,
            arrivals: Int,
        ) = runs.save(DayRun.snapshot(key, today, tally, arrivals))

        override fun clear() = runs.clear()
    }
}
