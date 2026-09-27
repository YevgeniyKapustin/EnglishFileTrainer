package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.db.entities.Answer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

data class Streak(
    val current: Int,
    val best: Int,
)

data class Week(
    val days: Int,
    val answers: Int,
    val minutes: Int,
)

data class WeakSpot(
    val chapter: Chapter,
    val section: Section,
    val accuracy: Double,
)

object Stats {
    const val BOOK_CHAPTERS = 36
    const val HEAT_WEEKS = 17
    const val WEAK_WINDOW_DAYS = 14L
    const val WEAK_MIN_ANSWERS = 5
    const val WEAK_BELOW = 0.9
    const val WEAK_MAX = 3
    const val BAD_BELOW = 0.65
    private const val PAUSE_SECONDS = 180L
    private const val SECONDS_PER_MINUTE = 60
    private const val LEVEL_LOW = 20
    private const val LEVEL_MID = 60
    private const val LEVEL_HIGH = 3

    fun streak(
        days: Set<LocalDate>,
        today: LocalDate,
    ): Streak {
        var day = if (today in days) today else today.minusDays(1)
        var current = 0
        while (day in days) {
            current++
            day = day.minusDays(1)
        }
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (d in days.sorted()) {
            run = if (previous != null && d == previous.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = d
        }
        return Streak(current, best)
    }

    fun level(count: Int): Int =
        when {
            count == 0 -> 0
            count < LEVEL_LOW -> 1
            count < LEVEL_MID -> 2
            else -> LEVEL_HIGH
        }

    private fun heatStart(today: LocalDate): LocalDate =
        today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks(HEAT_WEEKS - 1L)

    fun heatMonths(today: LocalDate): List<Month?> {
        val mondays = (0 until HEAT_WEEKS).map { heatStart(today).plusWeeks(it.toLong()) }
        return mondays.mapIndexed { i, monday ->
            monday.month.takeIf { i > 0 && it != mondays[i - 1].month }
        }
    }

    fun heat(
        answers: List<Answer>,
        today: LocalDate,
    ): List<List<Int?>> {
        val perDay = answers.groupingBy { it.day }.eachCount()
        val start = heatStart(today)
        return (0 until HEAT_WEEKS).map { w ->
            (0 until DayOfWeek.entries.size).map { d ->
                val day = start.plusWeeks(w.toLong()).plusDays(d.toLong())
                if (day.isAfter(today)) null else level(perDay[day] ?: 0)
            }
        }
    }

    fun week(
        answers: List<Answer>,
        today: LocalDate,
    ): Week {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisWeek = answers.filter { !it.day.isBefore(monday) && !it.day.isAfter(today) }
        val seconds =
            thisWeek
                .groupBy { it.day }
                .values
                .sumOf { day ->
                    day
                        .map { it.second }
                        .sorted()
                        .zipWithNext { a, b -> minOf(b - a, PAUSE_SECONDS) }
                        .sum()
                }
        return Week(
            days = thisWeek.map { it.day }.distinct().size,
            answers = thisWeek.size,
            minutes = (seconds / SECONDS_PER_MINUTE).toInt(),
        )
    }

    fun weakSpots(
        answers: List<Answer>,
        today: LocalDate,
        locate: (String) -> Pair<Chapter, Section>?,
    ): List<WeakSpot> {
        val from = today.minusDays(WEAK_WINDOW_DAYS - 1)
        return answers
            .filter { !it.day.isBefore(from) }
            .mapNotNull { a -> locate(a.exerciseId)?.let { it to a.correct } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size >= WEAK_MIN_ANSWERS }
            .map { (where, results) ->
                WeakSpot(
                    where.first,
                    where.second,
                    results.count { it }.toDouble() / results.size,
                )
            }.filter { it.accuracy < WEAK_BELOW }
            .sortedBy { it.accuracy }
            .take(WEAK_MAX)
    }
}
