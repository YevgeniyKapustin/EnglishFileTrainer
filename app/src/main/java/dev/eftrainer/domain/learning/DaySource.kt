package dev.eftrainer.domain.learning

import dev.eftrainer.core.time.Clock
import dev.eftrainer.data.AnswerLog
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.settings.DaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn

data class DayInputs(
    val review: ReviewInputs,
    val day: DayState,
)

data class DayPlan(
    val step: DayStep,
    val review: ReviewPlan,
    val exercises: List<Exercise>,
)

class DaySource(
    private val review: ReviewSource,
    private val chapters: List<Chapter>,
    answers: AnswerLog,
    private val settings: DaySettings,
    private val clock: Clock,
    scope: CoroutineScope,
) {
    val inputs: SharedFlow<DayInputs> =
        combine(
            review.inputs,
            settings.chosenChapter,
            settings.reviewedOn,
            answers.firstAnswered,
        ) { reviewInputs, chosen, reviewedOn, firstAnswered ->
            DayInputs(reviewInputs, DayState(chosen, reviewedOn, firstAnswered))
        }.shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val latest: DayInputs? get() = inputs.replayCache.lastOrNull()

    fun plan(inputs: DayInputs): DayPlan {
        val reviewPlan = review.plan(inputs.review)
        val progress = inputs.review.progress
        val step =
            DayFlow.step(chapters, inputs.review.completed, progress, reviewPlan, inputs.day, clock.today())
        return DayPlan(step, reviewPlan, DayFlow.exercises(step, reviewPlan, progress, inputs.review.ai))
    }

    fun choose(chapterId: String) = settings.choose(chapterId)

    fun reviewFinished() = settings.markReviewed(clock.today())
}
