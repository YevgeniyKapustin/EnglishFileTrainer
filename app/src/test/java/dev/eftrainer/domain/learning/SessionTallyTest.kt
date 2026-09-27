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
}
