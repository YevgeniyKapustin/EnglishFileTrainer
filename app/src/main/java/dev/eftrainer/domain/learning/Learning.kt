package dev.eftrainer.domain.learning

import dev.eftrainer.core.time.Clock
import dev.eftrainer.data.AnswerLog
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.exercises

class Learning(
    private val progress: ProgressRepository,
    private val chapters: ChapterRepository,
    private val clock: Clock,
    private val log: AnswerLog,
) {
    suspend fun answer(
        exercise: Exercise,
        correct: Boolean,
    ): Chapter? {
        val today = clock.today()
        progress.save(Fsrs.next(exercise.id, progress.get(exercise.id), correct, today))
        log.add(exercise.id, clock.now(), correct)

        return ChapterCompletion
            .current(chapters.chapters, chapters.getCompleted())
            ?.takeIf { c ->
                c.exercises.any { it.id == exercise.id } &&
                    ChapterCompletion.isComplete(c, progress.getAll())
            }?.also { chapters.markCompleted(it.id, today) }
    }
}
