package dev.eftrainer.domain.learning

import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object Fsrs {
    const val DESIRED_RETENTION = 0.9
    const val MAX_INTERVAL_DAYS = 180

    private val INITIAL_STABILITY = doubleArrayOf(0.212, 1.2931, 2.3065, 8.2956)
    private const val INITIAL_DIFFICULTY = 6.4133
    private const val INITIAL_DIFFICULTY_RATING_EXP = 0.8334
    private const val DIFFICULTY_DELTA = 3.0194
    private const val DIFFICULTY_MEAN_REVERSION = 0.001
    private const val RECALL_GROWTH = 1.8722
    private const val RECALL_STABILITY_DECAY = 0.1666
    private const val RECALL_RETRIEVABILITY_GAIN = 0.796
    private const val FORGET_BASE = 1.4835
    private const val FORGET_DIFFICULTY_EXP = 0.0614
    private const val FORGET_STABILITY_EXP = 0.2629
    private const val FORGET_RETRIEVABILITY_GAIN = 1.6483
    private const val SHORT_TERM_GAIN = 0.5425
    private const val SHORT_TERM_OFFSET = 0.0912
    private const val SHORT_TERM_STABILITY_DECAY = 0.0658
    private const val DECAY = 0.1542
    private const val MIN_DIFFICULTY = 1.0
    private const val MAX_DIFFICULTY = 10.0
    private const val AGAIN = 1
    private const val GOOD = 3
    private const val EASY = 4
    private const val MIN_STABILITY = 0.001

    private const val NEGATIVE_DECAY = -DECAY
    private val factor = 0.9.pow(1 / NEGATIVE_DECAY) - 1

    fun next(
        exerciseId: String,
        current: Progress?,
        correct: Boolean,
        today: LocalDate,
    ): Progress {
        val rating = if (correct) GOOD else AGAIN
        val stability: Double
        val difficulty: Double
        if (current == null) {
            stability = INITIAL_STABILITY[rating - 1]
            difficulty = initialDifficulty(rating).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        } else {
            val elapsed = ChronoUnit.DAYS.between(current.lastAnsweredOn, today).coerceAtLeast(0)
            val s = current.stability
            val d = current.difficulty
            stability =
                when {
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

    fun retrievability(
        elapsedDays: Long,
        stability: Double,
    ): Double = (1 + factor * elapsedDays / stability).pow(NEGATIVE_DECAY)

    fun interval(stability: Double): Int {
        val raw = stability / factor * (DESIRED_RETENTION.pow(1 / NEGATIVE_DECAY) - 1)
        return Math.rint(raw).toInt().coerceIn(1, MAX_INTERVAL_DAYS)
    }

    private fun initialDifficulty(rating: Int): Double =
        INITIAL_DIFFICULTY - exp(INITIAL_DIFFICULTY_RATING_EXP * (rating - 1)) + 1

    private fun nextDifficulty(
        d: Double,
        rating: Int,
    ): Double {
        val delta = -DIFFICULTY_DELTA * (rating - GOOD)
        val damped = d + (MAX_DIFFICULTY - d) * delta / (MAX_DIFFICULTY - MIN_DIFFICULTY)
        return (DIFFICULTY_MEAN_REVERSION * initialDifficulty(EASY) + (1 - DIFFICULTY_MEAN_REVERSION) * damped)
            .coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun shortTermStability(
        s: Double,
        rating: Int,
    ): Double {
        var increase = exp(SHORT_TERM_GAIN * (rating - GOOD + SHORT_TERM_OFFSET)) * s.pow(-SHORT_TERM_STABILITY_DECAY)
        if (rating >= GOOD) increase = max(increase, 1.0)
        return s * increase
    }

    private fun recallStability(
        s: Double,
        d: Double,
        r: Double,
    ): Double =
        s * (
            1 +
                exp(RECALL_GROWTH) * (MAX_DIFFICULTY + 1 - d) * s.pow(-RECALL_STABILITY_DECAY) *
                (exp((1 - r) * RECALL_RETRIEVABILITY_GAIN) - 1)
        )

    private fun forgetStability(
        s: Double,
        d: Double,
        r: Double,
    ): Double {
        val longTerm =
            FORGET_BASE * d.pow(-FORGET_DIFFICULTY_EXP) * ((s + 1).pow(FORGET_STABILITY_EXP) - 1) *
                exp((1 - r) * FORGET_RETRIEVABILITY_GAIN)
        val shortTerm = s / exp(SHORT_TERM_GAIN * SHORT_TERM_OFFSET)
        return min(longTerm, shortTerm)
    }
}
