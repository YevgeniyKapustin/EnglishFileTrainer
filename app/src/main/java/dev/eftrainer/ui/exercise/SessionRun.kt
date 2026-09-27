package dev.eftrainer.ui.exercise

import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.settings.TaskRuns
import dev.eftrainer.domain.learning.SessionTally

class SessionRun(
    private val runs: TaskRuns,
    private val taskId: String?,
) {
    fun tally(exercises: List<Exercise>): SessionTally =
        taskId?.let { SessionTally.resume(exercises, runs.load(it)) } ?: SessionTally(exercises)

    fun save(tally: SessionTally) {
        taskId?.let { runs.save(it, tally.run) }
    }

    fun clear() {
        taskId?.let(runs::clear)
    }
}
