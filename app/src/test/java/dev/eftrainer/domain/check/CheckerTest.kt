package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Rewrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckerTest {
    private val order =
        Rewrite(
            "t-order",
            "girlfriend / student / is / a / your",
            listOf("Is your girlfriend a student?"),
        )

    private val twoAnswers =
        Rewrite(
            "t-two",
            "What … ?",
            answers = listOf("What do you do?", "What's your job?"),
            hint = "(your job)",
        )

    private val correctMistake =
        Rewrite(
            "t-correct",
            "What time you get up?",
            answers = listOf("What time do you get up?"),
        )

    private val oneGap = Blank("t-gap", "Where {} born?", listOf(listOf("were you")), hint = "(you / be)")

    private val twoGaps =
        Blank(
            "t-gaps",
            "A: {} a car? B: No, but my wife {}.",
            listOf(listOf("Do you have", "Have you got"), listOf("does", "has")),
        )

    @Test fun `rewrite exact answer is correct`() =
        assertTrue(
            Checker.check(order, "Is your girlfriend a student?").correct,
        )

    @Test fun `rewrite ignores case, spaces and final punctuation`() =
        assertTrue(Checker.check(order, "  is your  girlfriend a student ").correct)

    @Test fun `rewrite wrong order is wrong and shows expected`() {
        val r = Checker.check(order, "Your girlfriend is a student?")
        assertFalse(r.correct)
        assertEquals("Is your girlfriend a student?", r.expected)
    }

    @Test fun `rewrite accepts any listed answer, expected is the first`() {
        assertTrue(Checker.check(twoAnswers, "what's your job").correct)
        assertTrue(Checker.check(twoAnswers, "What’s your job?").correct)
        assertEquals("What do you do?", Checker.check(twoAnswers, "x").expected)
    }

    @Test fun `rewrite empty input is wrong`() = assertFalse(Checker.check(order, "").correct)

    @Test fun `prompt left unchanged is wrong when there is a mistake`() =
        assertFalse(Checker.check(correctMistake, correctMistake.prompt).correct)

    @Test fun `prompt fixed is correct`() =
        assertTrue(
            Checker.check(correctMistake, "What time do you get up?").correct,
        )

    @Test fun `wrong word inside is wrong`() =
        assertFalse(Checker.check(correctMistake, "What time does you get up?").correct)

    @Test fun `blank correct`() {
        val r = Checker.check(oneGap, listOf("Were you"))
        assertTrue(r.correct)
        assertEquals(emptySet<Int>(), r.wrongGaps)
    }

    @Test fun `blank wrong shows filled text`() {
        val r = Checker.check(oneGap, listOf("you were"))
        assertFalse(r.correct)
        assertEquals(setOf(0), r.wrongGaps)
        assertEquals("Where were you born?", r.expected)
    }

    @Test fun `two gaps, any variant per gap`() {
        assertTrue(Checker.check(twoGaps, listOf("do you have", "does")).correct)
        assertTrue(Checker.check(twoGaps, listOf("Have you got", "has")).correct)
    }

    @Test fun `two gaps, only wrong one is marked`() {
        val r = Checker.check(twoGaps, listOf("Do you have", "do"))
        assertFalse(r.correct)
        assertEquals(setOf(1), r.wrongGaps)
        assertEquals("A: Do you have a car? B: No, but my wife does.", r.expected)
    }

    @Test fun `two gaps, both wrong`() = assertEquals(setOf(0, 1), Checker.check(twoGaps, listOf("", "")).wrongGaps)

    @Test(expected = IllegalArgumentException::class)
    fun `blank with wrong number of inputs fails loudly`() {
        Checker.check(twoGaps, listOf("Do you have"))
    }
}
