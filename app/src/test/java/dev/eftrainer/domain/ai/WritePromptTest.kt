package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Write
import dev.eftrainer.domain.ai.WritePrompt.Correction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritePromptTest {
    private val write = Write(
        "t-write", "Answer the questions about you.",
        questions = listOf("Where do you live?", "What did you do last night?"),
        criteria = listOf("answers every question in a full sentence"),
    )

    @Test fun `plain json with corrections is parsed`() {
        val g = WritePrompt.parseGrade(
            """{"pass": false, "summary": "Check your past simple.", "corrections": [
                {"mistake": "I watch TV last night", "correction": "I watched TV last night"},
                {"mistake": "I live in Moscow city", "correction": "I live in Moscow"}]}""",
        )!!
        assertFalse(g.pass)
        assertEquals("Check your past simple.", g.summary)
        assertEquals(
            listOf(Correction("I watch TV last night", "I watched TV last night"), Correction("I live in Moscow city", "I live in Moscow")),
            g.corrections,
        )
    }

    @Test fun `code fences, text around and escapes are tolerated`() {
        val raw = "Sure!\n```json\n{\"corrections\": [{\"correction\": \"\\\"did you go\\\"\", \"mistake\": \"\\\"you went\\\"\"}], \"summary\": \"Nice.\\nWell done\", \"pass\": \"TRUE\"}\n```"
        val g = WritePrompt.parseGrade(raw)!!
        assertTrue(g.pass)
        assertEquals("Nice.\nWell done", g.summary)
        assertEquals(listOf(Correction("\"you went\"", "\"did you go\"")), g.corrections)
    }

    @Test fun `no corrections field or empty list is fine`() {
        assertEquals(WritePrompt.Grade(true, "Great.", emptyList()), WritePrompt.parseGrade("""{"pass": true, "summary": "Great."}"""))
        assertEquals(emptyList<Correction>(), WritePrompt.parseGrade("""{"pass": true, "summary": "Great.", "corrections": []}""")!!.corrections)
    }

    @Test fun `half-empty and unchanged corrections are dropped, list is capped`() {
        val items = (1..9).joinToString(",") { """{"mistake": "a$it", "correction": "b$it"}""" }
        val g = WritePrompt.parseGrade(
            """{"pass": false, "summary": "s", "corrections": [{"mistake": "x", "correction": ""}, {"mistake": "same", "correction": "same"}, $items]}""",
        )!!
        assertEquals(WritePrompt.MAX_CORRECTIONS, g.corrections.size)
        assertEquals(Correction("a1", "b1"), g.corrections.first())
    }

    @Test fun `broken answers give null`() {
        assertNull(WritePrompt.parseGrade("The student passed."))
        assertNull(WritePrompt.parseGrade("""{"summary": "ok", "corrections": []}"""))
        assertNull(WritePrompt.parseGrade("""{"pass": true, "summary": " "}"""))
    }

    @Test fun `grading prompt has questions, criteria, rules and the text`() {
        val (system, user) = WritePrompt.grading(write, "Answer the questions in c about you", "QuASI", "I live in Kazan.")
        assertEquals("system", system.first)
        assertTrue(system.second.contains("JSON"))
        val text = user.second
        for (part in listOf("Answer the questions in c about you", "1 Where do you live?", "2 What did you do last night?", "- answers every question", "QuASI", "I live in Kazan.")) {
            assertTrue(part, text.contains(part))
        }
    }
}
