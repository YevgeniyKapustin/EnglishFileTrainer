package dev.eftrainer.ui.exercise.inputs

import dev.eftrainer.data.content.Blank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlankLayoutsTest {
    private fun blank(text: String) = Blank("b", text, List(text.split("{}").size - 1) { listOf("x") })

    @Test fun `comma options split into words`() {
        assertEquals(
            KeyWord("woman", listOf("medium height", "slim", "beautiful", "bald")),
            KeyWord.parse(blank("woman: medium height, slim, beautiful, bald → {}")),
        )
    }

    @Test fun `letter options split on spaces`() {
        assertEquals(KeyWord("tree /iː/", listOf("G", "V", "R")), KeyWord.parse(blank("tree /iː/: G  V  R → {}")))
    }

    @Test fun `key without options`() {
        assertEquals(KeyWord("play", emptyList()), KeyWord.parse(blank("play → {}")))
    }

    @Test fun `ordinary gaps are not key words`() {
        assertNull(KeyWord.parse(blank("friendly ≠ {}")))
        assertNull(KeyWord.parse(blank("Paragraph 2: {}")))
        assertNull(KeyWord.parse(blank("A person who talks a lot is {}.")))
    }

    @Test fun `dialogue lines split on line breaks`() {
        val lines = dialogueLines(tokenize("{} Louisa {} to marry?\nIn a few years."))
        assertEquals(2, lines.size)
        assertEquals(2, lines[0].count { it is Token.Gap })
        assertEquals(0, lines[1].count { it is Token.Gap })
    }
}
