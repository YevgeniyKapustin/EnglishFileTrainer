package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

/** «Освоено» = 3 верных подряд в разные дни (plans/learning.md). */
object Mastery {
    const val STREAK_TO_MASTER = 3

    fun isMastered(progress: Progress?): Boolean = progress != null && progress.streak >= STREAK_TO_MASTER

    /**
     * Серия растёт на верном ответе, только если сегодня упражнение ещё не отвечали:
     * повтор в тот же день (в т.ч. после ошибки и показа ответа) серию не двигает.
     * Неверный ответ сбрасывает серию.
     */
    fun nextStreak(current: Progress?, correct: Boolean, today: LocalDate): Int = when {
        !correct -> 0
        current == null -> 1
        current.lastAnsweredOn < today -> current.streak + 1
        else -> current.streak
    }

    /** Доля освоенных упражнений, 0.0..1.0. */
    fun share(exercises: List<Exercise>, progress: Map<String, Progress>): Double =
        if (exercises.isEmpty()) 0.0
        else exercises.count { isMastered(progress[it.id]) }.toDouble() / exercises.size
}
