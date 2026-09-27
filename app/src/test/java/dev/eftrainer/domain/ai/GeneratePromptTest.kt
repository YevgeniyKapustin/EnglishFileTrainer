package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.AnswerSource
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.exercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratePromptTest {
    private val book =
        listOf(
            Blank("b-1", "Where {} born?", listOf(listOf("were you")), hint = "(you / be)"),
            Rewrite("b-2", "live / you / where / do", listOf("Where do you live?")),
        )

    private fun parse(
        text: String,
        max: Int = 10,
    ) = GeneratedResponse.parse(text, "1A-grammar", book, emptyList(), max)

    @Test fun `valid items parsed, fenced json accepted`() {
        val reply =
            """
            Here you are:
            ```json
            {"items":[
              {"type":"blank","instruction":"Complete the questions.","text":"What {} {} last night?","gaps":[["did you"],["do"]],"hint":"(you / do)"},
              {"type":"rewrite","prompt":"from / she / is / where","answers":["Where is she from?","Where's she from?"]}
            ]}
            ```
            """.trimIndent()
        val items = parse(reply)
        assertEquals(2, items.size)
        val b = items[0].exercise as Blank
        assertEquals(listOf(listOf("did you"), listOf("do")), b.gaps)
        assertEquals("Complete the questions.", items[0].instruction)
        assertEquals(AnswerSource.MODEL, b.source)
        assertEquals(listOf("Where is she from?", "Where's she from?"), (items[1].exercise as Rewrite).answers)
        assertTrue(items.all { it.sectionId == "1A-grammar" })
    }

    @Test fun `bad items dropped`() {
        val long = "long ".repeat(40)
        val reply =
            """
            {"items":[
                {"type":"blank","text":"Where {} {}?","gaps":[["a"]]},
                {"type":"blank","text":"No gaps here","gaps":[]},
                {"type":"blank","text":"Is {} ok?","gaps":[[" "]]},
                {"type":"rewrite","prompt":"x","answers":[]},
                {"type":"rewrite","prompt":"Where do you live?","answers":["Where do you live"]},
                {"type":"choice","prompt":"a or b","answers":["a"]},
                {"type":"rewrite","prompt":"LONG","answers":["a"]},
                "junk"
            ]}
            """.trimIndent().replace("LONG", long)
        assertEquals(emptyList<Any>(), parse(reply))
    }

    @Test fun `duplicates of book, existing and within batch dropped`() {
        val reply =
            """
            {"items":[
                {"type":"blank","text":"Where {} born ?","gaps":[["were you"]]},
                {"type":"rewrite","prompt":"do / where / live / you","answers":["where do you live"]},
                {"type":"blank","text":"Who {} with?","gaps":[["do you live"]]},
                {"type":"blank","text":"Who {} with?","gaps":[["do you live"]]},
                {"type":"blank","text":"Was it {}?","gaps":[["fun"]]}
            ]}
            """.trimIndent()
        val existing = listOf(Blank("1A-grammar-gen-0001", "Was it {}?", listOf(listOf("fun"))))
        val items = GeneratedResponse.parse(reply, "1A-grammar", book, existing, 10)
        assertEquals(listOf("Who {} with?"), items.map { (it.exercise as Blank).text })
    }

    @Test fun `not json gives nothing, max respected`() {
        assertEquals(emptyList<Any>(), parse("Sorry, I can't."))
        val many =
            (1..5).joinToString(
                ",",
                """{"items":[""",
                "]}",
            ) { """{"type":"blank","text":"Q$it {}?","gaps":[["a"]]}""" }
        assertEquals(3, parse(many, max = 3).size)
    }

    @Test fun `prompt has rules, studied lessons, book sample with answers and count`() {
        val chapter = Chapters.all.first()
        val section = chapter.sections.first()
        val (system, user) = GeneratePrompt.build(chapter, section, listOf(chapter), section.exercises, book, 6)
        assertEquals("system", system.first)
        assertTrue("JSON" in system.second)
        val text = user.second
        assertTrue(text.contains("Studied lessons: 1A"))
        assertTrue(text.contains("Rules of this section:"))
        assertTrue(text.contains("Where ___ born? (you / be) -> Where were you born?"))
        assertTrue(text.endsWith("Write 6 new exercises."))
    }

    @Test fun `sample spreads over the book`() {
        assertEquals(listOf(0, 25, 50, 75), GeneratePrompt.sample((0 until 100).toList(), 4))
        assertEquals(listOf(1, 2), GeneratePrompt.sample(listOf(1, 2), 4))
    }
}
