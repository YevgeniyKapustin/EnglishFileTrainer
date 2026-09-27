package dev.eftrainer.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryTest {
    private fun answer(vararg days: Pair<Long, Boolean>) =
        days.fold(null as dev.eftrainer.data.db.entities.Progress?) { p, (day, ok) ->
            Fsrs.next("x", p, ok, TODAY.plusDays(day))
        }!!

    @Test fun `three correct on different days is mastered`() {
        val p = answer(0L to true, 1L to true, 3L to true)
        assertEquals(3, p.streak)
        assertTrue(Mastery.isMastered(p))
    }

    @Test fun `three correct on the same day is not`() {
        val p = answer(0L to true, 0L to true, 0L to true)
        assertEquals(1, p.streak)
        assertFalse(Mastery.isMastered(p))
    }

    @Test fun `wrong answer resets streak`() {
        val p = answer(0L to true, 1L to true, 2L to false)
        assertEquals(0, p.streak)
    }

    @Test fun `correct right after a mistake the same day does not count`() {
        val p = answer(0L to true, 1L to false, 1L to true)
        assertEquals(0, p.streak)
        assertEquals(1, answer(0L to true, 1L to false, 1L to true, 2L to true).streak)
    }

    @Test fun `share counts mastered`() {
        val s = section("s", 4)
        val progress = listOf(mastered("s-1"), mastered("s-2"), progress("s-3", streak = 2)).byId()
        assertEquals(0.5, Mastery.share(s.tasks.single().exercises, progress), 1e-9)
    }
}
