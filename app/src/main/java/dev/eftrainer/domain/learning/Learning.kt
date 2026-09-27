package dev.eftrainer.domain.learning

import dev.eftrainer.core.time.Clock
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress

/**
 * Запись ответа: FSRS + серия → Room сразу, затем проверка «глава пройдена».
 * Единственное место, где меняется прогресс.
 */
class Learning(
    private val progress: ProgressRepository,
    private val chapters: ChapterRepository,
    private val clock: Clock,
) {
    /** Возвращает главу, если этим ответом она стала пройденной (→ экран «Chapter done»). */
    suspend fun answer(exercise: Exercise, correct: Boolean): Chapter? {
        val today = clock.today()
        progress.save(Fsrs.next(exercise.id, progress.get(exercise.id), correct, today))

        val completed = chapters.getCompleted()
        val current = ChapterCompletion.current(chapters.chapters, completed) ?: return null
        if (current.exercises.none { it.id == exercise.id }) return null

        val all: Map<String, Progress> = progress.getAll()
        if (!ChapterCompletion.isComplete(current, all)) return null
        chapters.markCompleted(current.id, today)
        return current
    }
}
