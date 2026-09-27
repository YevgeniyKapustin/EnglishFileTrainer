package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Speak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Speak с группой букв (WB 1A 3b): распознаватель пишет буквы словами — проверка мягкая. */
class LetterCheckTest {

    private fun letters(text: String) = Speak("t-${text.replace(" ", "")}", text, listOf(text))
    private fun ok(text: String, said: String) = Checker.check(letters(text), said).correct

    @Test fun `letter group is detected`() {
        assertEquals("ake", Checker.letterGroup("A K E"))
        assertEquals("qiu", Checker.letterGroup("Q, I, U"))
        assertNull(Checker.letterGroup("Where do you live?"))
        assertNull(Checker.letterGroup("I am"))
    }

    @Test fun `typed letters pass`() {
        assertTrue(ok("A K E", "A K E"))
        assertTrue(ok("A K E", "ake"))
        assertTrue(ok("A K E", "a, k, e."))
        assertTrue(ok("A K E", "A-K-E"))
    }

    @Test fun `letter names from the recognizer pass`() {
        assertTrue(ok("A K E", "hey kay e"))
        assertTrue(ok("A K E", "ay kay ee"))
        assertTrue(ok("G V R", "gee vee are"))
        assertTrue(ok("N B F", "en bee ef"))
        assertTrue(ok("H P J", "aitch pee jay"))
        assertTrue(ok("X S K", "ex ess kay"))
        assertTrue(ok("M C D", "em see dee"))
        assertTrue(ok("M C D", "M sea D"))
        assertTrue(ok("Q I U", "queue eye you"))
        assertTrue(ok("Q I U", "cue I you"))
        assertTrue(ok("T O L", "tea oh el"))
        assertTrue(ok("W Y Z", "double you why zed"))
        assertTrue(ok("W Y Z", "double u y zee"))
    }

    @Test fun `okay is O and K`() = assertTrue(ok("O K", "okay"))

    @Test fun `wrong letters fail`() {
        assertFalse(ok("A K E", "a k i"))
        assertFalse(ok("G V R", "gee vee"))
        assertFalse(ok("Q I U", "queue eye you are"))
        assertFalse(ok("M C D", "hello"))
    }

    @Test fun `best of several hypotheses is taken`() {
        val e = letters("G V R")
        assertEquals("Jeep ere", Checker.bestHypothesis(e, listOf("Jeep ere", "gee we are")))
        assertEquals("gee vee are", Checker.bestHypothesis(e, listOf("Jeep ere", "gee vee are", "g v r")))
    }

    @Test fun `ordinary phrases are not lenient`() {
        // "you" ≠ "u" за пределами групп букв.
        assertFalse(Checker.check(Speak("t", "See you", listOf("See you")), "c u").correct)
    }
}
