package dev.eftrainer.ui.exercise.inputs

import org.junit.Assert.assertEquals
import org.junit.Test

class CapitalsTest {
    @Test fun `first letter of a sentence is capital`() {
        assertEquals("Where did you go?", Capitals.fix("where did you go?", sentenceStart = true))
        assertEquals("  Yes", Capitals.fix("  yes", sentenceStart = true))
    }

    @Test fun `gap inside a sentence keeps lower case`() =
        assertEquals(
            "went",
            Capitals.fix("went", sentenceStart = false),
        )

    @Test fun `next sentence starts with a capital`() =
        assertEquals("Yes. It was great!", Capitals.fix("yes. it was great!", sentenceStart = true))

    @Test fun `pronoun i is capital`() {
        assertEquals("Yes, I do", Capitals.fix("yes, i do", sentenceStart = true))
        assertEquals(
            "I'm tired and I’ve had enough",
            Capitals.fix("i'm tired and i’ve had enough", sentenceStart = true),
        )
        assertEquals("so I.", Capitals.fix("so i.", sentenceStart = false))
    }

    @Test fun `i while typing a word stays lower case`() {
        assertEquals("I", Capitals.fix("i", sentenceStart = true))
        assertEquals("and i", Capitals.fix("and i", sentenceStart = false))
        assertEquals("taxi is", Capitals.fix("taxi is", sentenceStart = false))
    }

    @Test fun `gaps at the start of a sentence or after a label`() =
        assertEquals(
            listOf(true, false, true, true),
            Capitals.gapStarts("{} you like it? Yes, I {}. {} A: {} here"),
        )
}
