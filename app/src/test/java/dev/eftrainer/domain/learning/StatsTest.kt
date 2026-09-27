package dev.eftrainer.domain.learning

import dev.eftrainer.data.db.entities.Answer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset

class StatsTest {
    private fun answer(
        id: String,
        day: LocalDate = TODAY,
        second: Long = day.atStartOfDay().toEpochSecond(ZoneOffset.UTC),
        correct: Boolean = true,
    ) = Answer(exerciseId = id, day = day, second = second, correct = correct)

    private fun days(vararg back: Long) = back.map { TODAY.minusDays(it) }.toSet()

    @Test fun `streak survives until today is over`() {
        assertEquals(Streak(3, 3), Stats.streak(days(0, 1, 2), TODAY))
        assertEquals(Streak(2, 2), Stats.streak(days(1, 2), TODAY))
        assertEquals(Streak(0, 2), Stats.streak(days(2, 3), TODAY))
    }

    @Test fun `best streak is the longest run`() {
        assertEquals(Streak(1, 4), Stats.streak(days(0, 5, 6, 7, 8, 10), TODAY))
    }

    @Test fun `heat ends with this week and leaves future days empty`() {
        val grid = Stats.heat(listOf(answer("a"), answer("b")), TODAY)
        assertEquals(Stats.HEAT_WEEKS, grid.size)
        val todayIndex = TODAY.dayOfWeek.value - 1
        assertEquals(1, grid.last()[todayIndex])
        grid.last().drop(todayIndex + 1).forEach { assertNull(it) }
    }

    @Test fun `months are marked on the first week that starts in them`() {
        val months = Stats.heatMonths(TODAY)
        assertEquals(Stats.HEAT_WEEKS, months.size)
        assertNull(months.first())
        assertEquals(Month.MARCH, months.last { it != null })
    }

    @Test fun `week counts days, answers and minutes with long pauses capped`() {
        val start = TODAY.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        val answers =
            listOf(
                answer("a", second = start),
                answer("b", second = start + 60),
                answer("c", second = start + 60 + 3600),
                answer("d", day = TODAY.minusWeeks(2)),
            )
        assertEquals(Week(days = 1, answers = 3, minutes = 4), Stats.week(answers, TODAY))
    }

    @Test fun `weak spots are the worst recent sections with enough answers`() {
        val a = section("a", 1)
        val b = section("b", 1)
        val ch = chapter("1A", a, b)
        val answers =
            List(5) { answer("a-$it", correct = it < 2) } +
                List(5) { answer("b-$it", correct = it < 4) } +
                List(9) { answer("a-old", day = TODAY.minusDays(20), correct = false) }
        val spots = Stats.weakSpots(answers, TODAY) { id -> ch to if (id.startsWith("a")) a else b }
        assertEquals(listOf(a to 0.4, b to 0.8), spots.map { it.section to it.accuracy })
    }
}
