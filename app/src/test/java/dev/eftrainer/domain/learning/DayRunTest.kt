package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Rewrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayRunTest {
    private val first = Rewrite("a", "p", listOf("x"))
    private val second = Rewrite("b", "p", listOf("y"))
    private val third = Rewrite("c", "p", listOf("z"))
    private val fresh = Rewrite("1A-g-gen-0001", "p", listOf("w"))
    private val known: Map<String, Exercise> = listOf(first, second, third, fresh).associateBy { it.id }
    private val key = DayRun.keyOf(DayStep.Review)

    private fun saved(
        tally: SessionTally,
        arrivals: Int = 0,
    ) = DayRun.snapshot(key, TODAY, tally, arrivals)

    @Test fun `an untouched session resumes at its first exercise`() {
        val tally = SessionTally(listOf(second, first, third))
        val resumed = saved(tally).resume(key, TODAY, known::get)
        assertEquals(second, resumed?.tally?.current)
        assertEquals(3, resumed?.tally?.total)
    }

    @Test fun `a session resumes at the exercise it was left on`() {
        val tally = SessionTally(listOf(first, second, third))
        tally.answer(first, correct = true, inputs = listOf("x"))
        tally.next(correct = true)
        val resumed = saved(tally).resume(key, TODAY, known::get)?.tally
        assertEquals(second, resumed?.current)
        assertEquals(1, resumed?.done)
        assertEquals(3, resumed?.total)
    }

    @Test fun `a wrong answer stays at the end of the queue after resuming`() {
        val tally = SessionTally(listOf(first, second, third))
        tally.answer(first, correct = false, inputs = listOf("q"))
        tally.next(correct = false)
        val resumed = saved(tally).resume(key, TODAY, known::get)?.tally ?: error("not resumed")
        assertEquals(second, resumed.current)
        assertEquals(listOf(first), resumed.mistakes)
        assertEquals(listOf("a", "b", "c").toSet(), resumed.seenIds)
        resumed.answer(second, correct = true, inputs = listOf("y"))
        resumed.next(correct = true)
        resumed.answer(third, correct = true, inputs = listOf("z"))
        resumed.next(correct = true)
        assertEquals(first, resumed.current)
    }

    @Test fun `fresh arrivals and their count survive`() {
        val tally = SessionTally(listOf(first))
        tally.add(listOf(fresh))
        val resumed = saved(tally, arrivals = 1).resume(key, TODAY, known::get) ?: error("not resumed")
        assertEquals(1, resumed.arrivals)
        assertEquals(setOf("a", fresh.id), resumed.tally.seenIds)
        assertEquals(emptyList<Exercise>(), TrainingQueue.arrivals(listOf(fresh), resumed.tally.seenIds, 1))
    }

    @Test fun `a run from another day is dropped`() {
        assertNull(saved(SessionTally(listOf(first))).resume(key, TODAY.plusDays(1), known::get))
    }

    @Test fun `a run from another step is dropped`() {
        val continueKey = DayRun.keyOf(DayStep.Continue(chapter("1B")))
        assertNotEquals(key, continueKey)
        assertNull(saved(SessionTally(listOf(first))).resume(continueKey, TODAY, known::get))
    }

    @Test fun `a run with a vanished exercise is dropped`() {
        val gone = Rewrite("gone", "p", listOf("x"))
        assertNull(saved(SessionTally(listOf(first, gone))).resume(key, TODAY, known::get))
    }

    @Test fun `a finished run is dropped`() {
        val tally = SessionTally(listOf(first))
        tally.answer(first, correct = true, inputs = listOf("x"))
        tally.next(correct = true)
        assertNull(saved(tally).resume(key, TODAY, known::get))
    }

    @Test fun `practice and continue of one chapter have different keys`() {
        val c = chapter("1B")
        assertNotEquals(DayRun.keyOf(DayStep.Practice(c)), DayRun.keyOf(DayStep.Continue(c)))
    }
}
