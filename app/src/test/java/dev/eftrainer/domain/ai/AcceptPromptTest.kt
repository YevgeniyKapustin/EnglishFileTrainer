package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptPromptTest {
    private val rewrite =
        Rewrite(
            "t-rewrite",
            "What … ?",
            listOf("What did you do last night?"),
            hint = "(you / do last night)",
        )
    private val blank =
        Blank(
            "t-blank",
            "A: {} you {} to the party?\nB: No, I was ill.",
            listOf(listOf("Did"), listOf("go", "come")),
            hint = "(go)",
        )
    private val task =
        Task(
            "t",
            Origin.WB,
            "Write questions in the past simple",
            listOf(rewrite, blank),
            context = "Anna had a party on Saturday.",
            words = listOf("go", "come"),
        )

    private fun user(prompt: List<Pair<String, String>>): String = prompt.single { it.first == "user" }.second

    private fun system(prompt: List<Pair<String, String>>): String = prompt.single { it.first == "system" }.second

    @Test fun `a rewrite prompt carries the task, the key, the answer and the rules`() {
        val prompt = AcceptPrompt.asking(task, rewrite, "  What have you done last night? ", "Past simple: did + verb")
        val user = user(prompt)
        assertTrue(user.contains("Rules of this lesson:\nPast simple: did + verb"))
        assertTrue(user.contains("Task: Write questions in the past simple"))
        assertTrue(user.contains("Text: Anna had a party on Saturday."))
        assertTrue(user.contains("Word list: go, come"))
        assertTrue(user.contains("Exercise: What … ?"))
        assertTrue(user.contains("Hint: (you / do last night)"))
        assertTrue(user.contains("Key answers: What did you do last night?"))
        assertTrue(user.endsWith("Student's answer: What have you done last night?"))
        assertTrue(system(prompt).contains("strict"))
        assertTrue(system(prompt).contains("not completely sure"))
    }

    @Test fun `a blank prompt shows the gaps, their keys and the student's words`() {
        val user = user(AcceptPrompt.asking(task, blank, listOf("Did", " went "), ""))
        assertFalse(user.contains("Rules of this lesson"))
        assertTrue(user.contains("Exercise (fill the gaps ___): A: ___ you ___ to the party?\nB: No, I was ill."))
        assertTrue(user.contains("Key answer: A: Did you go to the party?"))
        assertTrue(user.contains("Key for gap 1: Did"))
        assertTrue(user.contains("Key for gap 2: go | come"))
        assertTrue(user.contains("Student's gap 2: went"))
        assertTrue(user.contains("Student's answer: A: Did you went to the party?"))
    }

    @Test fun `accept is read from json with text and fences around it`() {
        assertEquals(true, AcceptPrompt.parseAccepted("""{"accept": true, "reason": "Same meaning."}"""))
        assertEquals(
            false,
            AcceptPrompt.parseAccepted(
                """
                ```json
                {"reason": "Wrong tense.", "accept": false}
                ```
                """.trimIndent(),
            ),
        )
        assertEquals(true, AcceptPrompt.parseAccepted("""Sure! {"Accept": "TRUE"}"""))
    }

    @Test fun `no clear accept is no answer`() {
        assertNull(AcceptPrompt.parseAccepted("Yes, it is correct."))
        assertNull(AcceptPrompt.parseAccepted("""{"accept": "maybe"}"""))
        assertNull(AcceptPrompt.parseAccepted(""))
    }
}
