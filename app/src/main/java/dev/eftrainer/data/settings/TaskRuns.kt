package dev.eftrainer.data.settings

import android.content.Context
import androidx.core.content.edit
import dev.eftrainer.data.generated.Json
import dev.eftrainer.domain.learning.TaskRun

class TaskRuns(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("task_runs", Context.MODE_PRIVATE)

    fun load(taskId: String): TaskRun = prefs.getString(taskId, null)?.let(TaskRunCodec::decode) ?: TaskRun()

    fun save(
        taskId: String,
        run: TaskRun,
    ) = prefs.edit { putString(taskId, TaskRunCodec.encode(run)) }

    fun clear(taskId: String) = prefs.edit { remove(taskId) }
}

object TaskRunCodec {
    private const val PASSED = "passed"
    private const val MISSED = "missed"
    private const val SKIPPED = "skipped"

    fun encode(run: TaskRun): String =
        Json.write(
            mapOf(
                PASSED to run.passed,
                MISSED to run.missed.toList(),
                SKIPPED to run.skipped.toList(),
            ),
        )

    fun decode(text: String): TaskRun? {
        val fields = runCatching { Json.parse(text) }.getOrNull() as? Map<*, *> ?: return null
        val passed =
            (fields[PASSED] as? Map<*, *>)
                .orEmpty()
                .entries
                .associate { (id, inputs) -> id.toString() to (inputs as? List<*>).orEmpty().map { it.toString() } }
        return TaskRun(passed, idsOf(fields[MISSED]), idsOf(fields[SKIPPED]))
    }

    private fun idsOf(value: Any?): Set<String> = (value as? List<*>).orEmpty().map { it.toString() }.toSet()
}
