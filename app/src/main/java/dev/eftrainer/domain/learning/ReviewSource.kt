package dev.eftrainer.domain.learning

import dev.eftrainer.core.time.Clock
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.SkipLog
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.data.generated.GeneratedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import java.time.LocalDate

data class ReviewInputs(
    val completed: Set<String>,
    val progress: Map<String, Progress>,
    val ai: Boolean,
    val generated: Map<String, List<Exercise>>,
    val skipped: Set<String> = emptySet(),
) {
    fun plan(
        chapters: List<Chapter>,
        today: LocalDate,
        verbs: List<Exercise> = emptyList(),
    ): ReviewPlan = TrainingQueue.review(chapters, completed, progress, today, ai, generated, verbs, skipped)
}

class ReviewSource(
    private val chapters: ChapterRepository,
    progress: ProgressRepository,
    generated: GeneratedRepository,
    skips: SkipLog,
    ai: Flow<Boolean>,
    private val clock: Clock,
    scope: CoroutineScope,
) {
    val inputs: SharedFlow<ReviewInputs> =
        combine(
            chapters.completed,
            progress.progress,
            ai.distinctUntilChanged(),
            generated.observeBySection(),
            skips.skipped,
            ::ReviewInputs,
        ).shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val latest: ReviewInputs? get() = inputs.replayCache.lastOrNull()

    fun plan(inputs: ReviewInputs): ReviewPlan = inputs.plan(chapters.chapters, clock.today(), IrregularVerbs.exercises)
}
