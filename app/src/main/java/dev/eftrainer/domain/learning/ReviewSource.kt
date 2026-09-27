package dev.eftrainer.domain.learning

import dev.eftrainer.core.time.Clock
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.data.generated.GeneratedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

/** Всё, от чего зависит состав Review. */
data class ReviewInputs(
    val completed: Set<String>,
    val progress: Map<String, Progress>,
    val ai: Boolean,                                  // ключ + сеть: разговоры с ИИ входят в сессию
    val generated: Map<String, List<Exercise>>,       // свежие упражнения ИИ по id раздела
) {
    fun plan(chapters: List<Chapter>, today: LocalDate): ReviewPlan =
        TrainingQueue.review(chapters, completed, progress, today, ai, generated)
}

/**
 * Один источник состава Review и для счётчика на главном экране, и для самой сессии —
 * чтобы «12 exercises» над кнопкой всегда совпадало с тем, что покажет сессия.
 */
class ReviewSource(
    private val chapters: ChapterRepository,
    progress: ProgressRepository,
    generated: GeneratedRepository,
    ai: Flow<Boolean>,
    private val clock: Clock,
) {
    /** Обновляется после каждого ответа, пачки свежих упражнений и смены ключа/сети. */
    val inputs: Flow<ReviewInputs> =
        combine(chapters.completed, progress.progress, ai.distinctUntilChanged(), generated.observeBySection(), ::ReviewInputs)

    fun plan(inputs: ReviewInputs): ReviewPlan = inputs.plan(chapters.chapters, clock.today())
}
