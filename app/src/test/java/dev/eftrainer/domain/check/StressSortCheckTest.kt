package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Sort
import dev.eftrainer.data.content.Stress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StressSortCheckTest {
    private val across = Stress("t-across", parts = listOf("a", "cross"), stressed = listOf(1))

    private val party =
        Stress(
            "t-party",
            parts = listOf("It ", "was", " a great party. ", "Was", " it at Kim’s house?"),
            stressed = listOf(3),
            targets = listOf(1, 3),
        )

    private val weak =
        Stress("t-weak", parts = listOf("We ", "were", " at school."), stressed = emptyList(), targets = listOf(1))

    private val bird =
        Sort("t-bird", word = "f[ir]st", columns = listOf("1 computer /ə/", "2 bird /ɜː/"), answer = 1)

    @Test fun `word mode is a syllable pick`() {
        assertTrue(across.syllables)
        assertFalse(party.syllables)
    }

    @Test fun `stressed syllable is correct`() {
        val result = TapChecker.check(across, listOf("1"))
        assertTrue(result.correct)
        assertEquals("aCROSS", result.expected)
    }

    @Test fun `wrong syllable marks both syllables`() {
        val result = TapChecker.check(across, listOf("0"))
        assertFalse(result.correct)
        assertEquals(setOf(0, 1), result.wrongGaps)
    }

    @Test fun `sentence needs exactly the stressed words`() {
        assertTrue(TapChecker.check(party, listOf("3")).correct)
        assertEquals(setOf(1), TapChecker.check(party, listOf("1", "3")).wrongGaps)
        assertEquals(setOf(3), TapChecker.check(party, emptyList()).wrongGaps)
        assertEquals("It was a great party. WAS it at Kim’s house?", TapChecker.check(party, emptyList()).expected)
    }

    @Test fun `all weak forms means tapping nothing`() {
        assertTrue(TapChecker.check(weak, emptyList()).correct)
        assertFalse(TapChecker.check(weak, listOf("1")).correct)
    }

    @Test fun `stress model rejects bad indices`() {
        assertThrows(IllegalArgumentException::class.java) { across.copy(stressed = listOf(0, 1)) }
        assertThrows(IllegalArgumentException::class.java) { party.copy(stressed = listOf(2)) }
        assertThrows(IllegalArgumentException::class.java) { party.copy(targets = listOf(1, 7)) }
    }

    @Test fun `right column is correct`() {
        val result = TapChecker.check(bird, listOf("1"))
        assertTrue(result.correct)
        assertEquals("first → 2 bird /ɜː/", result.expected)
    }

    @Test fun `wrong or missing column is wrong`() {
        assertFalse(TapChecker.check(bird, listOf("0")).correct)
        assertFalse(TapChecker.check(bird, emptyList()).correct)
    }

    @Test fun `plain word drops pink letters and syllable marks`() {
        assertEquals("hurry", bird.copy(word = "hu|rry").plainWord)
        assertEquals("stopped", bird.copy(word = "stopp[ed]").plainWord)
    }

    @Test fun `sort model rejects bad columns`() {
        assertThrows(IllegalArgumentException::class.java) { bird.copy(answer = 2) }
        assertThrows(IllegalArgumentException::class.java) { bird.copy(columns = listOf("only")) }
    }
}
