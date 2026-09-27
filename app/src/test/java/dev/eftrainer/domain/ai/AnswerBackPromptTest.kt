package dev.eftrainer.domain.ai

import dev.eftrainer.domain.ai.WritePrompt.Correction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerBackPromptTest {
    @Test fun `the question and the trimmed answer go to the user message`() {
        val messages = AnswerBackPrompt.grading("Where do you live?", "  I live in Moscow.  ")
        assertEquals(listOf("system", "user"), messages.map { it.first })
        assertEquals("Question: Where do you live?\nStudent's answer: I live in Moscow.", messages[1].second)
        assertTrue(messages[0].second.contains("\"pass\""))
    }

    @Test fun `a grade with corrections is parsed`() {
        val g =
            AnswerBackPrompt.parseGrade(
                """
                ```json
                {"pass": false, "summary": "You need the past simple.",
                 "corrections": [{"mistake": "I go to the cinema", "correction": "I went to the cinema"}]}
                ```
                """.trimIndent(),
            )!!
        assertFalse(g.pass)
        assertEquals("You need the past simple.", g.summary)
        assertEquals(listOf(Correction("I go to the cinema", "I went to the cinema")), g.corrections)
    }

    @Test fun `a pass without corrections is parsed`() {
        val g = AnswerBackPrompt.parseGrade("""{"pass": true, "summary": "Good answer.", "corrections": []}""")!!
        assertTrue(g.pass)
        assertTrue(g.corrections.isEmpty())
    }

    @Test fun `text without a grade is not parsed`() {
        assertNull(AnswerBackPrompt.parseGrade("Sorry, I can't help with that."))
        assertNull(AnswerBackPrompt.parseGrade("""{"summary": "no pass field"}"""))
    }
}
