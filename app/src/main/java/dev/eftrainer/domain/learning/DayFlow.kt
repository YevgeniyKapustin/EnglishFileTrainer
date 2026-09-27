package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

sealed interface DayStep {
    val chapter: Chapter? get() = null

    data object Review : DayStep

    data class Continue(
        override val chapter: Chapter,
    ) : DayStep

    data class Practice(
        override val chapter: Chapter,
    ) : DayStep

    data object Rest : DayStep
}

data class DayState(
    val chosenChapter: String? = null,
    val reviewedOn: LocalDate? = null,
    val firstAnswered: Map<String, LocalDate> = emptyMap(),
    val skipped: Set<String> = emptySet(),
)

object DayFlow {
    fun step(
        chapters: List<Chapter>,
        completed: Set<String>,
        progress: Map<String, Progress>,
        plan: ReviewPlan,
        day: DayState,
        today: LocalDate,
    ): DayStep =
        if (reviewPending(plan, day.reviewedOn, today)) {
            DayStep.Review
        } else {
            actual(chapters, completed, progress, day, today) ?: restOrReview(plan)
        }

    fun reviewPending(
        plan: ReviewPlan,
        reviewedOn: LocalDate?,
        today: LocalDate,
    ): Boolean =
        plan.due.isNotEmpty() ||
            plan.verbs.isNotEmpty() ||
            (reviewedOn != today && plan.current.isNotEmpty())

    fun actual(
        chapters: List<Chapter>,
        completed: Set<String>,
        progress: Map<String, Progress>,
        day: DayState,
        today: LocalDate,
    ): DayStep? {
        val anchor =
            chapters.indexOfFirst { it.id == day.chosenChapter }.takeIf { it >= 0 }
                ?: chapters.indexOfFirst { it.id !in completed }.takeIf { it >= 0 }
                ?: return null
        return chapters.drop(anchor).firstNotNullOfOrNull { chapter ->
            when {
                !ChapterCompletion.isComplete(chapter, progress, day.skipped) -> DayStep.Continue(chapter)
                finishedOn(chapter, day.firstAnswered, today) -> DayStep.Practice(chapter)
                else -> null
            }
        }
    }

    fun exercises(
        step: DayStep,
        plan: ReviewPlan,
        progress: Map<String, Progress>,
        ai: Boolean,
        skipped: Set<String> = emptySet(),
    ): List<Exercise> =
        when (step) {
            DayStep.Review -> plan.all
            is DayStep.Continue -> continueQueue(step.chapter, progress, ai, skipped)
            is DayStep.Practice -> practiceQueue(step.chapter, progress)
            DayStep.Rest -> emptyList()
        }

    fun continueQueue(
        chapter: Chapter,
        progress: Map<String, Progress>,
        ai: Boolean = false,
        skipped: Set<String> = emptySet(),
    ): List<Exercise> =
        chapter.sections
            .flatMap { TrainingQueue.firstPass(it, progress, ai, skipped) }
            .take(TrainingQueue.SESSION_SIZE)

    fun left(
        chapter: Chapter,
        progress: Map<String, Progress>,
        skipped: Set<String> = emptySet(),
    ): Int = chapter.exercises.count { it.id !in progress && it.id !in skipped }

    fun practiceQueue(
        chapter: Chapter,
        progress: Map<String, Progress>,
    ): List<Exercise> =
        chapter.exercises
            .sortedWith(
                compareBy<Exercise>(
                    { Mastery.isMastered(progress[it.id]) },
                    { progress[it.id]?.let { p -> p.correctCount - p.wrongCount } ?: 0 },
                ),
            ).take(TrainingQueue.SESSION_SIZE)

    private fun finishedOn(
        chapter: Chapter,
        firstAnswered: Map<String, LocalDate>,
        today: LocalDate,
    ): Boolean = chapter.exercises.any { firstAnswered[it.id] == today }

    private fun restOrReview(plan: ReviewPlan): DayStep = if (plan.all.isEmpty()) DayStep.Rest else DayStep.Review
}
