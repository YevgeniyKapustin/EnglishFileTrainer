package dev.eftrainer.domain.learning

import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Порт FSRS-6 (py-fsrs 6, параметры по умолчанию, без обучения на своих данных).
 * Ответ → оценка: неверно = Again, верно = Good. День — календарный (plans/learning.md).
 * Сверено с py-fsrs в FsrsTest.
 */
object Fsrs {
    const val DESIRED_RETENTION = 0.9
    const val MAX_INTERVAL_DAYS = 180

    private val w = doubleArrayOf(
        0.212, 1.2931, 2.3065, 8.2956,
        6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835,
        0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658,
        0.1542,
    )
    private const val AGAIN = 1
    private const val GOOD = 3
    private const val EASY = 4
    private const val MIN_STABILITY = 0.001

    private val decay = -w[20]
    private val factor = 0.9.pow(1 / decay) - 1

    fun next(exerciseId: String, current: Progress?, correct: Boolean, today: LocalDate): Progress {
        val rating = if (correct) GOOD else AGAIN
        val stability: Double
        val difficulty: Double
        if (current == null) {
            stability = w[rating - 1]
            difficulty = initialDifficulty(rating).coerceIn(1.0, 10.0)
        } else {
            val elapsed = ChronoUnit.DAYS.between(current.lastAnsweredOn, today).coerceAtLeast(0)
            val s = current.stability
            val d = current.difficulty
            stability = when {
                elapsed < 1 -> shortTermStability(s, rating)
                correct -> recallStability(s, d, retrievability(elapsed, s))
                else -> forgetStability(s, d, retrievability(elapsed, s))
            }
            difficulty = nextDifficulty(d, rating)
        }
        val s = max(stability, MIN_STABILITY)
        return Progress(
            exerciseId = exerciseId,
            streak = Mastery.nextStreak(current, correct, today),
            stability = s,
            difficulty = difficulty,
            dueOn = today.plusDays(interval(s).toLong()),
            lastAnsweredOn = today,
            correctCount = (current?.correctCount ?: 0) + if (correct) 1 else 0,
            wrongCount = (current?.wrongCount ?: 0) + if (correct) 0 else 1,
        )
    }

    /** Шанс вспомнить через [elapsedDays] дней при стабильности [stability]. */
    fun retrievability(elapsedDays: Long, stability: Double): Double =
        (1 + factor * elapsedDays / stability).pow(decay)

    /** Через сколько дней шанс вспомнить упадёт до DESIRED_RETENTION. */
    fun interval(stability: Double): Int {
        val raw = stability / factor * (DESIRED_RETENTION.pow(1 / decay) - 1)
        return Math.rint(raw).toInt().coerceIn(1, MAX_INTERVAL_DAYS)   // rint = round как в Python
    }

    private fun initialDifficulty(rating: Int): Double = w[4] - exp(w[5] * (rating - 1)) + 1

    private fun nextDifficulty(d: Double, rating: Int): Double {
        val delta = -w[6] * (rating - 3)
        val damped = d + (10 - d) * delta / 9
        return (w[7] * initialDifficulty(EASY) + (1 - w[7]) * damped).coerceIn(1.0, 10.0)
    }

    private fun shortTermStability(s: Double, rating: Int): Double {
        var increase = exp(w[17] * (rating - 3 + w[18])) * s.pow(-w[19])
        if (rating >= GOOD) increase = max(increase, 1.0)
        return s * increase
    }

    private fun recallStability(s: Double, d: Double, r: Double): Double =
        s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp((1 - r) * w[10]) - 1))

    private fun forgetStability(s: Double, d: Double, r: Double): Double {
        val longTerm = w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp((1 - r) * w[14])
        val shortTerm = s / exp(w[17] * w[18])
        return min(longTerm, shortTerm)
    }
}
