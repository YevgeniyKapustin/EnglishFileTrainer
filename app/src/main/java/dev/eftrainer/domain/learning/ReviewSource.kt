package dev.eftrainer.domain.learning

import dev.eftrainer.core.time.Clock
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.data.generated.GeneratedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

data class ReviewInputs(
    val completed: Set<String>,
    val progress: Map<String, Progress>,
    val ai: Boolean,
    val generated: Map<String, List<Exercise>>,
) {
    fun plan(
        chapters: List<Chapter>,
        today: LocalDate,
        verbs: List<Exercise> = emptyList(),
    ): ReviewPlan = TrainingQueue.review(chapters, completed, progress, today, ai, generated, verbs)
}

class ReviewSource(
    private val chapters: ChapterRepository,
    progress: ProgressRepository,
    generated: GeneratedRepository,
    ai: Flow<Boolean>,
    private val clock: Clock,
) {
    val inputs: Flow<ReviewInputs> =
        combine(
            chapters.completed,
            progress.progress,
            ai.distinctUntilChanged(),
            generated.observeBySection(),
            ::ReviewInputs,
        )

    fun plan(inputs: ReviewInputs): ReviewPlan = inputs.plan(chapters.chapters, clock.today(), IrregularVerbs.exercises)
}
