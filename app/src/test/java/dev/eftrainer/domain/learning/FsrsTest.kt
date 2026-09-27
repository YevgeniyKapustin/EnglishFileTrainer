package dev.eftrainer.domain.learning

import dev.eftrainer.data.db.entities.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FsrsTest {
    private val start = LocalDate.of(2026, 1, 1)

    private data class Step(
        val day: Long,
        val correct: Boolean,
        val stability: Double,
        val difficulty: Double,
        val dueDay: Long,
    )

    private fun replay(steps: List<Step>): Progress? {
        var p: Progress? = null
        for (s in steps) {
            val today = start.plusDays(s.day)
            p = Fsrs.next("x", p, s.correct, today)
            val where = "день ${s.day}, ${if (s.correct) "верно" else "неверно"}"
            assertEquals("$where: stability", s.stability, p.stability, 1e-4)
            assertEquals("$where: difficulty", s.difficulty, p.difficulty, 1e-4)
            assertEquals("$where: dueOn", start.plusDays(s.dueDay), p.dueOn)
            assertEquals(today, p.lastAnsweredOn)
        }
        return p
    }

    @Test fun `correct on due dates grows interval`() {
        replay(
            listOf(
                Step(0, true, 2.3065, 2.118104, 2),
                Step(2, true, 10.964332, 2.111214, 13),
                Step(12, true, 43.931669, 2.104331, 56),
                Step(50, true, 144.8331, 2.097455, 195),
            ),
        )
    }

    @Test fun `interval is capped at 180 days`() {
        replay(
            listOf(
                Step(0, true, 2.3065, 2.118104, 2),
                Step(2, true, 10.964332, 2.111214, 13),
                Step(12, true, 43.931669, 2.104331, 56),
                Step(50, true, 144.8331, 2.097455, 195),
                Step(180, true, 425.26423, 2.090586, 360),
                Step(360, true, 805.953875, 2.083724, 540),
            ),
        )
    }

    @Test fun `wrong first answer comes back next day`() {
        replay(
            listOf(
                Step(0, false, 0.212, 6.4133, 1),
                Step(1, true, 1.886788, 6.402115, 3),
                Step(3, true, 6.269161, 6.390941, 9),
            ),
        )
    }

    @Test fun `lapse drops back to about a day`() {
        replay(
            listOf(
                Step(0, true, 2.3065, 2.118104, 2),
                Step(2, true, 10.964332, 2.111214, 13),
                Step(12, false, 1.522138, 7.392238, 14),
                Step(13, true, 3.536602, 7.380074, 17),
            ),
        )
    }

    @Test fun `same day answers use short-term stability`() {
        replay(
            listOf(
                Step(0, true, 2.3065, 2.118104, 2),
                Step(0, true, 2.3065, 2.111214, 2),
                Step(0, false, 0.775084, 7.392238, 1),
                Step(0, true, 0.828165, 7.380074, 1),
            ),
        )
    }

    @Test fun `late answer still grows`() {
        replay(
            listOf(
                Step(0, true, 2.3065, 2.118104, 2),
                Step(30, true, 37.420638, 2.111214, 67),
            ),
        )
    }

    @Test fun `counts correct and wrong`() {
        var p: Progress? = null
        listOf(
            true,
            false,
            true,
            true,
        ).forEachIndexed { i, ok -> p = Fsrs.next("x", p, ok, start.plusDays(i.toLong())) }
        assertEquals(3, p!!.correctCount)
        assertEquals(1, p.wrongCount)
    }

    @Test fun `retrievability at interval equals desired retention`() {
        val r = Fsrs.retrievability(10, 10.0)
        assertTrue("r = $r", kotlin.math.abs(r - Fsrs.DESIRED_RETENTION) < 1e-9)
    }
}
