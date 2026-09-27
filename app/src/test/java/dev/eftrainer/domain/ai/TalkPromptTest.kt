package dev.eftrainer.domain.ai

import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.content.Picture
import dev.eftrainer.data.content.Talk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TalkPromptTest {
    private val talk =
        Talk(
            "t-talk",
            "You are a new classmate.",
            "Ask three questions about your partner.",
            "Hi! I'm Anna.",
            listOf("uses do/does in questions"),
        )

    @Test fun `plain json is parsed`() {
        assertEquals(
            TalkPrompt.Grade(true, "Good questions."),
            TalkPrompt.parseGrade("""{"pass": true, "summary": "Good questions."}"""),
        )
    }

    @Test fun `code fences and text around are tolerated`() {
        val raw =
            """
            Here you are:
            ```json
            {
              "pass": false,
              "summary": "You said \"Where you live?\".
            Say \"Where do you live?\""
            }
            ```
            """.trimIndent()
        val g = TalkPrompt.parseGrade(raw)!!
        assertFalse(g.pass)
        assertEquals(
            """
            You said "Where you live?".
            Say "Where do you live?"
            """.trimIndent(),
            g.summary,
        )
    }

    @Test fun `pass as a string and field order do not matter`() {
        val g = TalkPrompt.parseGrade("""{"summary":"Well done — nice","pass":"TRUE"}""")!!
        assertTrue(g.pass)
        assertEquals("Well done — nice", g.summary)
    }

    @Test fun `broken answers give null`() {
        assertNull(TalkPrompt.parseGrade("I think the student passed."))
        assertNull(TalkPrompt.parseGrade("""{"summary": "ok"}"""))
        assertNull(TalkPrompt.parseGrade("""{"pass": true, "summary": "  "}"""))
    }

    @Test fun `end mark is removed and ends only after a couple of turns`() {
        assertEquals(
            TalkPrompt.Reply("Nice to meet you. Bye!", true),
            TalkPrompt.reply("Nice to meet you. Bye! [END]", studentTurns = 3),
        )
        assertFalse(TalkPrompt.reply("Bye! [END]", studentTurns = 1).ended)
        assertEquals("Where are you from?", TalkPrompt.reply("Partner: **Where are you from?**", 1).text)
    }

    @Test fun `conversation starts with a user line and keeps roles`() {
        val m = TalkPrompt.conversation(talk, listOf(ChatLine(false, talk.opener), ChatLine(true, "Hi Anna")))
        assertEquals(listOf("system", "user", "assistant", "user"), m.map { it.first })
        assertTrue(m[0].second.contains(talk.situation))
    }

    @Test fun `grading prompt carries criteria and transcript`() {
        val (_, user) =
            TalkPrompt.grading(
                talk,
                "rule",
                listOf(ChatLine(false, "Hi!"), ChatLine(true, "Where you live?")),
            )
        assertTrue(user.second.contains("- uses do/does in questions"))
        assertTrue(user.second.contains("Student: Where you live?"))
        assertTrue(user.second.contains("Partner: Hi!"))
    }

    @Test fun `pictures reach the conversation and the grading as hidden captions`() {
        val pictured =
            talk.copy(
                pictures =
                    listOf(
                        Picture("1", "p_one", "A man is pouring milk."),
                        Picture("2", "p_two", "A woman is reading a letter."),
                    ),
            )
        val system = TalkPrompt.conversation(pictured, emptyList())[0].second
        val grading = TalkPrompt.grading(pictured, "", emptyList())[1].second
        for (text in listOf(system, grading)) {
            assertTrue(text, text.contains("- Picture 1: A man is pouring milk."))
            assertTrue(text, text.contains("- Picture 2: A woman is reading a letter."))
            assertFalse(text, text.contains("p_one"))
        }
    }

    @Test fun `no pictures leave the prompts unchanged`() {
        val system = TalkPrompt.conversation(talk, emptyList())[0].second
        val grading = TalkPrompt.grading(talk, "", emptyList())[1].second
        assertFalse(system.contains("Picture"))
        assertFalse(grading.contains("Picture"))
        assertTrue(system.endsWith("[END] at the very end."))
    }

    @Test fun `chat keeps context and follow-ups in order`() {
        val chat = AiChat(listOf("system" to "s", "user" to "q")).start().answer("a").ask(" more? ")
        assertEquals(listOf("system", "user", "assistant", "user"), chat.messages.map { it.first })
        assertEquals("more?", chat.messages.last().second)
        assertTrue(chat.loading)
        val failed = chat.fail(AiException.Reason.NETWORK)
        assertEquals(chat.messages, failed.start().messages)
    }
}
