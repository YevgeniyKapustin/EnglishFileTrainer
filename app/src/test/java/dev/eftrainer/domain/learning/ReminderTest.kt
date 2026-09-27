package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.exercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class ReminderTest {
    private val ch = chapter("1B", section("g", 2))
    private val plan = ReviewPlan(due = ch.sections[0].exercises.take(1), current = emptyList())
    private val empty = ReviewPlan(emptyList(), emptyList())

    @Test fun `review count when something is waiting`() {
        assertEquals(Nudge.Review(1), Reminder.nudge(emptyMap(), plan, ch, TODAY))
    }

    @Test fun `continue the chapter when review is empty`() {
        assertEquals(Nudge.Continue(ch), Reminder.nudge(emptyMap(), empty, ch, TODAY))
    }

    @Test fun `silent once practised today or book finished`() {
        val today = listOf(progress("g-1", lastAnsweredOn = TODAY)).byId()
        assertNull(Reminder.nudge(today, plan, ch, TODAY))
        assertNull(Reminder.nudge(emptyMap(), empty, null, TODAY))
    }

    @Test fun `delay runs to the next occurrence of the time`() {
        val at = LocalTime.of(19, 0)
        assertEquals(Duration.ofHours(2), Reminder.delayUntil(TODAY.atTime(17, 0), at))
        assertEquals(Duration.ofHours(23), Reminder.delayUntil(TODAY.atTime(20, 0), at))
        assertEquals(Duration.ofDays(1), Reminder.delayUntil(TODAY.atTime(19, 0), at))
    }
}
