package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalQuestionTest {
    private val byId =
        Chapters.all
            .flatMap { c -> c.sections.flatMap { s -> s.tasks.flatMap { it.exercises } } }
            .associateBy { it.id }

    private fun questionOf(id: String): String? = PersonalQuestion.questionIn(byId.getValue(id))

    @Test fun `questions to the learner in 1A qualify`() {
        assertEquals("Where do you live?", questionOf("1A-sb1a-03"))
        assertEquals("Do you live in a house or a flat?", questionOf("1A-sb1a-04"))
        assertEquals("Do you live with your parents or on your own?", questionOf("1A-vocabulary-x1-01"))
        assertEquals("Where does your father work?", questionOf("1A-sb2a-02"))
    }

    @Test fun `a dialogue item is asked by its question line`() {
        assertEquals("What did you do last weekend?", questionOf("1A-wb1b-02"))
        assertEquals("What’s the weather like where you live?", questionOf("1B-wb2a-03"))
    }

    @Test fun `a leading chart sign is dropped`() {
        assertEquals("What kind of person do you want to meet?", questionOf("1B-sb2a-03"))
    }

    @Test fun `statements, role play lines and answers inside one line do not qualify`() {
        listOf(
            "1B-wb2b-02",
            "1A-pe2b-01",
            "1A-pe3c-02",
            "1A-pe3e-02",
            "1B-vocabulary-x3-06",
            "1B-sb4a-02",
            "1B-sb2a-01",
            "1A-wb3c-03",
        ).forEach { assertNull(it, questionOf(it)) }
    }

    @Test fun `sentences without you do not qualify`() {
        assertNull(PersonalQuestion.questionIn(Blank("t-1", "My grandfather was {} in 1950.", listOf(listOf("born")))))
        assertNull(PersonalQuestion.questionIn(Blank("t-2", "Where {} she live?", listOf(listOf("does")))))
        assertNull(PersonalQuestion.questionIn(Rewrite("t-3", "and / you", listOf("And you?"))))
    }

    @Test fun `quoted lines, speaking and irregular verbs do not qualify`() {
        assertNull(PersonalQuestion.questionIn(Rewrite("t-4", "p", listOf("'Where are you going?' she asked."))))
        assertNull(PersonalQuestion.questionIn(Rewrite("t-5", "p", listOf("\"Do you like it?\""))))
        assertNull(PersonalQuestion.questionIn(Speak("t-6", "Where are you from?", listOf("Where are you from?"))))
        val verbs = IrregularVerbs.chapter.sections.flatMap { s -> s.tasks.flatMap { it.exercises } }
        assertTrue(verbs.isNotEmpty())
        verbs.forEach { assertNull(it.id, PersonalQuestion.questionIn(it)) }
    }

    @Test fun `both chapters have questions to answer`() {
        Chapters.all.forEach { chapter ->
            val count =
                chapter.sections
                    .flatMap { s -> s.tasks.flatMap { it.exercises } }
                    .count { PersonalQuestion.questionIn(it) != null }
            assertTrue(chapter.id, count >= MIN_PER_CHAPTER)
        }
    }

    private companion object {
        const val MIN_PER_CHAPTER = 10
    }
}
