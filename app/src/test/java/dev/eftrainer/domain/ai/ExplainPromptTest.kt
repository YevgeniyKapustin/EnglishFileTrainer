package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.File1A
import dev.eftrainer.data.content.Rewrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplainPromptTest {
    @Test fun `student answer is shown with gaps filled in`() {
        val e = Blank("t-01", "Where {} you {}?", listOf(listOf("do"), listOf("live")))
        assertEquals("Where does you live?", ExplainPrompt.answerText(e, listOf(" does", "live ")))
    }

    @Test fun `prompt carries the lesson rules and both answers`() {
        val section = File1A.sections.first()
        val task = section.tasks.first()
        val e = task.exercises.first()
        val (system, user) = ExplainPrompt.build(File1A, section, task, e, "wrong answer", "right answer")
        assertEquals("system", system.first)
        assertTrue(user.second.contains("Rules of this lesson"))
        assertTrue(user.second.contains("Student's answer: wrong answer"))
        assertTrue(user.second.contains("Correct answer: right answer"))
    }

    @Test fun `rewrite answer is the typed sentence`() {
        val e = Rewrite("t-02", "you / live / where", listOf("Where do you live?"))
        assertEquals("Where you live?", ExplainPrompt.answerText(e, listOf("Where you live? ")))
    }

    @Test fun `selection prompt carries the selected text, rules and studied lessons`() {
        val section = File1A.sections.first()
        val task = section.tasks.first()
        val ctx = ExplainPrompt.Context(File1A, section, listOf(File1A), task, task.exercises.first())
        val (_, user) = ExplainPrompt.selection(" do you ", ctx)
        assertTrue(user.second.contains("Explain this: \"do you\""))
        assertTrue(user.second.contains("Lessons studied so far: 1A"))
        assertTrue(user.second.contains("Task: ${task.instruction}"))
        if (section.reference.isNotEmpty()) assertTrue(user.second.contains("Rules of this section"))
    }
}
