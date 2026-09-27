package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.aiExercises
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.presets
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

data class ReviewPlan(
    val due: List<Exercise>,
    val current: List<Exercise>,
) {
    val all: List<Exercise> get() = due + current
}

object TrainingQueue {
    const val SESSION_SIZE = 20
    const val DUE_LIMIT = 10
    const val MASTERED_SHARE = 5
    const val FRESH_ARRIVALS = 5

    fun firstPass(
        section: Section,
        progress: Map<String, Progress>,
        ai: Boolean = false,
    ): List<Exercise> =
        section.tasks
            .flatMap { it.exercises }
            .filter { it in section.exercises || (ai && it in section.aiExercises) }
            .filter { it.id !in progress }

    fun freshOf(
        section: Section,
        generated: Map<String, List<Exercise>>,
    ): List<Exercise> = section.presets + generated[section.id].orEmpty()

    fun arrivals(
        fresh: List<Exercise>,
        seen: Set<String>,
        alreadyAdded: Int,
    ): List<Exercise> = fresh.filterNot { it.id in seen }.take((FRESH_ARRIVALS - alreadyAdded).coerceAtLeast(0))

    fun review(
        chapters: List<Chapter>,
        completed: Set<String>,
        progress: Map<String, Progress>,
        today: LocalDate,
        ai: Boolean = false,
        generated: Map<String, List<Exercise>> = emptyMap(),
    ): ReviewPlan {
        fun answeredToday(e: Exercise) = progress[e.id]?.lastAnsweredOn == today

        val due =
            chapters
                .filter { it.id in completed }
                .flatMap { c -> c.exercises + c.sections.flatMap { freshOf(it, generated) } }
                .filter { e -> progress[e.id]?.let { it.dueOn <= today } == true && !answeredToday(e) }
                .sortedBy { progress.getValue(it.id).dueOn }
                .take(DUE_LIMIT)

        val chapter = ChapterCompletion.current(chapters, completed) ?: return ReviewPlan(due, emptyList())
        val room = SESSION_SIZE - due.size

        val open = ChapterCompletion.activeSections(chapter, progress)
        val candidates =
            open
                .flatMap { if (ai) it.exercises + it.aiExercises else it.exercises }
                .filterNot(::answeredToday)
        val fresh =
            open
                .filter { GenerationPlan.bookMastered(it, progress) }
                .flatMap { freshOf(it, generated) }
                .filterNot(::answeredToday)
        val (mastered, unmastered) = candidates.partition { Mastery.isMastered(progress[it.id]) }
        val (freshMastered, freshUnmastered) = fresh.partition { Mastery.isMastered(progress[it.id]) }

        val weakFirst =
            unmastered.sortedWith(
                compareBy<Exercise>(
                    { it.id !in progress },
                    { progress[it.id]?.streak ?: 0 },
                    { progress[it.id]?.lastAnsweredOn },
                ),
            )
        val freshFirst =
            freshUnmastered.sortedWith(
                compareBy<Exercise>(
                    { it.id in progress },
                    { progress[it.id]?.streak ?: 0 },
                    { progress[it.id]?.lastAnsweredOn },
                ),
            )
        val oldestMastered = (mastered + freshMastered).sortedBy { progress.getValue(it.id).lastAnsweredOn }

        val mixCount = minOf(oldestMastered.size, room / MASTERED_SHARE)
        val main = (weakFirst + freshFirst).take(room - mixCount)
        val mix = oldestMastered.take(mixCount)

        return ReviewPlan(due, interleave(main, mix))
    }

    private fun interleave(
        main: List<Exercise>,
        mix: List<Exercise>,
    ): List<Exercise> {
        if (mix.isEmpty()) return main
        val step = main.size / (mix.size + 1) + 1
        val result = mutableListOf<Exercise>()
        var m = 0
        main.forEachIndexed { i, e ->
            result += e
            if ((i + 1) % step == 0 && m < mix.size) result += mix[m++]
        }
        while (m < mix.size) result += mix[m++]
        return result
    }
}
