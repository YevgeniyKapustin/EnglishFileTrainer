package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTallyTest {
    private val speak = Speak("s", "Where are you from?", listOf("Where are you from?"))
    private val rewrite = Rewrite("r", "p", listOf("a"))
    private val first = Rewrite("a", "p", listOf("x"))
    private val second = Rewrite("b", "p", listOf("y"))
    private val third = Rewrite("c", "p", listOf("z"))
    private val task = listOf(first, second, third)

    @Test fun `skip records nothing and leaves the session`() {
        val tally = SessionTally(listOf(speak, rewrite))
        tally.skip()
        assertEquals(rewrite, tally.current)
        assertEquals(1, tally.total)
        assertEquals(0, tally.done)
        assertEquals(emptyList<Any>(), tally.mistakes)
        assertFalse("s" in tally.seenIds)
    }

    @Test fun `skipping everything finishes with nothing counted`() {
        val tally = SessionTally(listOf(speak))
        tally.skip()
        assertTrue(tally.finished)
        assertEquals(0, tally.total)
        assertEquals(0, tally.firstTry)
    }

    @Test fun `skip after a wrong answer keeps that mistake`() {
        val tally = SessionTally(listOf(speak))
        assertTrue(tally.answer(speak, correct = false, inputs = listOf("where you from")))
        tally.next(correct = false)
        tally.skip()
        assertTrue(tally.finished)
        assertEquals(1, tally.total)
        assertEquals(listOf(speak), tally.mistakes)
    }

    @Test fun `only the first answer is recorded and wrong ones come back`() {
        val tally = SessionTally(listOf(rewrite))
        assertTrue(tally.answer(rewrite, correct = false, inputs = listOf("b")))
        tally.next(correct = false)
        assertEquals(rewrite, tally.current)
        assertFalse(tally.answer(rewrite, correct = true, inputs = listOf("a")))
        tally.next(correct = true)
        assertTrue(tally.finished)
        assertEquals(1, tally.done)
        assertEquals(0, tally.firstTry)
        assertEquals(listOf("a"), tally.correctInputs(listOf("r")))
    }

    @Test fun `a resumed run starts with the remaining items in book order`() {
        val tally = SessionTally.resume(task, TaskRun(passed = mapOf("b" to listOf("y"))))
        assertEquals(first, tally.current)
        tally.answer(first, correct = true, inputs = listOf("x"))
        tally.next(correct = true)
        assertEquals(third, tally.current)
        assertEquals(3, tally.total)
        assertEquals(2, tally.done)
        assertEquals(listOf("y"), tally.correctInputs(listOf("b")))
    }

    @Test fun `a missed item stays queued and counts as a mistake`() {
        val tally = SessionTally.resume(task, TaskRun(passed = mapOf("a" to listOf("x")), missed = setOf("b")))
        assertEquals(second, tally.current)
        assertEquals(listOf(second), tally.mistakes)
        assertFalse(tally.answer(second, correct = true, inputs = listOf("y")))
        assertEquals(2, tally.done)
        assertEquals(2, tally.firstTry)
    }

    @Test fun `skipped items stay out of the queue and the total`() {
        val tally = SessionTally.resume(listOf(speak, rewrite), TaskRun(skipped = setOf("s")))
        assertEquals(rewrite, tally.current)
        assertEquals(1, tally.total)
        assertEquals(0, tally.done)
    }

    @Test fun `the run reflects what was passed, missed and skipped`() {
        val tally = SessionTally(listOf(speak, first, second))
        tally.skip()
        tally.answer(first, correct = false, inputs = listOf("q"))
        tally.next(correct = false)
        tally.answer(second, correct = true, inputs = listOf("y"))
        assertEquals(TaskRun(mapOf("b" to listOf("y")), setOf("a"), setOf("s")), tally.run)
    }

    @Test fun `resuming the saved run continues the same session`() {
        val before = SessionTally(task)
        before.answer(first, correct = true, inputs = listOf("x"))
        before.next(correct = true)
        before.answer(second, correct = false, inputs = listOf("q"))
        val after = SessionTally.resume(task, before.run)
        assertEquals(second, after.current)
        assertEquals(before.done, after.done)
        assertEquals(before.total, after.total)
        assertEquals(before.mistakes, after.mistakes)
    }

    @Test fun `a run with nothing left starts a fresh practice`() {
        val passedAll = TaskRun(passed = task.associate { it.id to listOf("x") })
        val tally = SessionTally.resume(task, passedAll)
        assertEquals(first, tally.current)
        assertEquals(0, tally.done)
        assertEquals(3, tally.total)
    }

    @Test fun `ids that left the task are ignored`() {
        val tally = SessionTally.resume(task, TaskRun(passed = mapOf("gone" to listOf("x"), "a" to listOf("x"))))
        assertEquals(second, tally.current)
        assertEquals(1, tally.done)
        assertEquals(3, tally.total)
    }
}
