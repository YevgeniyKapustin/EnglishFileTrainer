package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.aiExercises
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.needsAi
import dev.eftrainer.data.content.presets
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

data class ReviewPlan(
    val due: List<Exercise>,
    val current: List<Exercise>,
    val verbs: List<Exercise> = emptyList(),
) {
    val all: List<Exercise> get() = due + verbs + current
}

object TrainingQueue {
    const val SESSION_SIZE = 20
    const val DUE_LIMIT = 10
    const val MASTERED_SHARE = 5
    const val FRESH_ARRIVALS = 5
    const val NEW_VERBS_PER_DAY = 5
    const val VERB_LIMIT = 10
    const val VERB_SHARE = 5
    const val BOOK_RUN = 3

    fun firstPass(
        section: Section,
        progress: Map<String, Progress>,
        ai: Boolean = false,
        skipped: Set<String> = emptySet(),
    ): List<Exercise> =
        section.tasks
            .flatMap { it.exercises }
            .filter { it in section.exercises || (ai && it in section.aiExercises) }
            .filter { it.id !in progress && it.id !in skipped }

    fun task(
        section: Section,
        taskId: String,
        ai: Boolean = false,
    ): List<Exercise> =
        section.tasks
            .filter { it.id == taskId }
            .flatMap { it.exercises }
            .filter { ai || !it.needsAi }

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
        verbs: List<Exercise> = emptyList(),
        skipped: Set<String> = emptySet(),
    ): ReviewPlan {
        val due =
            chapters
                .filter { it.id in completed }
                .flatMap { c -> c.exercises + c.sections.flatMap { freshOf(it, generated) } }
                .filter { e -> progress[e.id]?.let { it.dueOn <= today && it.lastAnsweredOn != today } == true }
                .sortedBy { progress.getValue(it.id).dueOn }
                .take(DUE_LIMIT)

        val verbPool = verbs(verbs, progress, today)
        val room = SESSION_SIZE - due.size - minOf(verbPool.size, VERB_SHARE)
        val current =
            ChapterCompletion
                .current(chapters, completed)
                ?.let { CurrentReview.plan(ReviewScope(it, progress, today, ai, generated, skipped), room) }
                .orEmpty()
        return ReviewPlan(due, current, verbPool.take(SESSION_SIZE - due.size - current.size))
    }

    fun verbs(
        verbs: List<Exercise>,
        progress: Map<String, Progress>,
        today: LocalDate,
    ): List<Exercise> {
        val due =
            verbs
                .filter { v -> progress[v.id]?.let { it.dueOn <= today && it.lastAnsweredOn != today } == true }
                .sortedBy { progress.getValue(it.id).dueOn }
                .take(VERB_LIMIT)
        val introducedToday = verbs.count { v -> progress[v.id]?.let { firstAnsweredToday(it, today) } == true }
        val newToday = minOf(NEW_VERBS_PER_DAY - introducedToday, VERB_LIMIT - due.size).coerceAtLeast(0)
        return due + verbs.filter { it.id !in progress }.take(newToday)
    }

    private fun firstAnsweredToday(
        progress: Progress,
        today: LocalDate,
    ): Boolean = progress.lastAnsweredOn == today && progress.correctCount + progress.wrongCount == 1
}
