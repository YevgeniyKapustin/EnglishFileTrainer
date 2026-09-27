package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.optional
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

data class ReviewScope(
    val chapter: Chapter,
    val progress: Map<String, Progress>,
    val today: LocalDate,
    val ai: Boolean,
    val generated: Map<String, List<Exercise>>,
    val skipped: Set<String>,
)

object CurrentReview {
    private data class Run(
        val group: String,
        val items: List<Exercise>,
        val fresh: Boolean,
    )

    fun plan(
        scope: ReviewScope,
        room: Int,
    ): List<Exercise> {
        val progress = scope.progress
        val runs =
            scope.chapter.sections.flatMap { section ->
                val fresh =
                    if (GenerationPlan.bookSeen(section, progress, scope.skipped)) {
                        TrainingQueue.freshOf(section, scope.generated).filterNot {
                            progress[it.id]?.lastAnsweredOn ==
                                scope.today
                        }
                    } else {
                        emptyList()
                    }
                if (fresh.isNotEmpty()) {
                    fresh.map { Run(it.id, listOf(it), fresh = true) }
                } else {
                    bookRuns(section, scope)
                }
            }
        val (mastered, unmastered) = runs.partition { run -> run.items.all { Mastery.isMastered(progress[it.id]) } }

        val weakFirst =
            unmastered.sortedWith(
                compareBy<Run>(
                    { run -> run.items.minOf { progress[it.id]?.streak ?: 0 } },
                    { run -> run.items.mapNotNull { progress[it.id]?.lastAnsweredOn }.minOrNull() },
                ),
            )
        val oldestMastered =
            mastered.sortedWith(
                compareBy<Run>(
                    { !it.fresh },
                    { run -> run.items.minOf { progress.getValue(it.id).lastAnsweredOn } },
                ),
            )

        val mix = fill(oldestMastered, room / TrainingQueue.MASTERED_SHARE)
        val main = fill(spread(weakFirst), room - mix.sumOf { it.items.size })
        return interleave(main, mix).flatMap { it.items }
    }

    private fun bookRuns(
        section: Section,
        scope: ReviewScope,
    ): List<Run> =
        section.tasks
            .filter { it.origin != Origin.GENERATED }
            .flatMap { task ->
                task.exercises
                    .filter { e ->
                        (scope.ai || !e.optional) &&
                            scope.progress[e.id]?.let { it.lastAnsweredOn != scope.today } == true
                    }.chunked(TrainingQueue.BOOK_RUN) { Run(task.id, it.toList(), fresh = false) }
            }

    private fun fill(
        runs: List<Run>,
        room: Int,
    ): List<Run> {
        val taken = mutableListOf<Run>()
        var left = room
        for (run in runs) {
            if (left <= 0) break
            val part = run.items.take(left)
            taken += run.copy(items = part)
            left -= part.size
        }
        return taken
    }

    private fun spread(runs: List<Run>): List<Run> {
        val left = runs.toMutableList()
        val result = mutableListOf<Run>()
        while (left.isNotEmpty()) {
            val next = left.indexOfFirst { it.group != result.lastOrNull()?.group }.coerceAtLeast(0)
            result += left.removeAt(next)
        }
        return result
    }

    private fun <T> interleave(
        main: List<T>,
        mix: List<T>,
    ): List<T> {
        if (mix.isEmpty()) return main
        val step = main.size / (mix.size + 1) + 1
        val result = mutableListOf<T>()
        var m = 0
        main.forEachIndexed { i, e ->
            result += e
            if ((i + 1) % step == 0 && m < mix.size) result += mix[m++]
        }
        while (m < mix.size) result += mix[m++]
        return result
    }
}
