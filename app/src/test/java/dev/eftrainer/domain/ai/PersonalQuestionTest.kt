package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Clip
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import dev.eftrainer.data.content.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalQuestionTest {
    private val tasks = Chapters.all.flatMap { c -> c.sections.flatMap { it.tasks } }
    private val byId = tasks.flatMap { t -> t.exercises.map { it.id to (it to t) } }.toMap()

    private fun questionOf(id: String): String? =
        byId.getValue(id).let { (exercise, task) -> PersonalQuestion.questionIn(exercise, task) }

    private fun alone(
        exercise: Exercise,
        task: Task = Task("t", Origin.WB, "Complete the questions", listOf(exercise)),
    ): String? = PersonalQuestion.questionIn(exercise, task)

    @Test fun `questions to the learner in 1A qualify`() {
        assertEquals("Where do you live?", questionOf("1A-sb1a-03"))
        assertEquals("Do you live in a house or a flat?", questionOf("1A-sb1a-04"))
        assertEquals("Do you live with your parents or on your own?", questionOf("1A-vocabulary-x1-01"))
        assertEquals("Where does your father work?", questionOf("1A-sb2a-02"))
        assertEquals("Where did you stay? What was it like?", questionOf("2A-sb5c-04"))
    }

    @Test fun `a leading chart sign is dropped`() {
        assertEquals("What kind of person do you want to meet?", questionOf("1B-sb2a-03"))
    }

    @Test fun `dialogue items that already hold the answer do not qualify`() {
        listOf(
            "1A-wb1b-02",
            "1A-vocabulary-x2-01",
            "1B-wb2a-03",
            "1C-gbb-02",
            "1C-gbb-06",
            "2A-gbb-01",
            "1A-wbpe3b-02",
        ).forEach { assertNull(it, questionOf(it)) }
    }

    @Test fun `questions answered by someone else in the task do not qualify`() {
        listOf("2A-wb3c-21", "2A-wb3c-31", "2A-wb3c-41", "2A-wb3c-51").forEach { assertNull(it, questionOf(it)) }
    }

    @Test fun `questions tied to a situation or another person do not qualify`() {
        listOf(
            "1A-wb2a-02",
            "1A-gba-05",
            "1A-gbb-03",
            "1B-wb1c-05",
            "1C-gba-02",
            "1C-rv1-01",
            "2A-vocabulary-x2-08",
            "2B-grammar-x1-02",
            "2B-gbb-02",
            "2B-wb2a-02",
            "2B-grammar-x4-04",
            "2C-vocabulary-x2-02",
        ).forEach { assertNull(it, questionOf(it)) }
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

    @Test fun `a task with a text, a picture or a whole recording does not qualify`() {
        val item = Blank("t-1", "Where do you {}?", listOf(listOf("live")))
        val task = Task("t", Origin.WB, "Complete", listOf(item))
        assertEquals("Where do you live?", alone(item, task))
        assertNull(alone(item, task.copy(context = "Anna lives in Rome.")))
        assertNull(alone(item, task.copy(dialogue = true)))
        assertNull(alone(item, task.copy(images = listOf("1a_photo"))))
        assertNull(alone(item, task.copy(video = "ep1_introduction")))
        assertNull(alone(item, task.copy(audio = "1.9")))
        assertNull(alone(item.copy(image = "1a_photo"), task))
        assertEquals("Where do you live?", alone(item.copy(clip = Clip(0, 1_000)), task.copy(audio = "1.2")))
    }

    @Test fun `sentences without you do not qualify`() {
        assertNull(alone(Blank("t-1", "My grandfather was {} in 1950.", listOf(listOf("born")))))
        assertNull(alone(Blank("t-2", "Where {} she live?", listOf(listOf("does")))))
        assertNull(alone(Rewrite("t-3", "and / you", listOf("And you?"))))
    }

    @Test fun `quoted lines, speaking and irregular verbs do not qualify`() {
        assertNull(alone(Rewrite("t-4", "p", listOf("'Where are you going?' she asked."))))
        assertNull(alone(Rewrite("t-5", "p", listOf("\"Do you like it?\""))))
        assertNull(alone(Speak("t-6", "Where are you from?", listOf("Where are you from?"))))
        IrregularVerbs.chapter.sections.flatMap { it.tasks }.also { assertTrue(it.isNotEmpty()) }.forEach { task ->
            task.exercises.forEach { assertNull(it.id, PersonalQuestion.questionIn(it, task)) }
        }
    }

    @Test fun `the book still has questions to answer`() {
        val count = { chapterIds: Set<String> ->
            Chapters.all
                .filter { it.id in chapterIds }
                .flatMap { c -> c.sections.flatMap { it.tasks } }
                .sumOf { task -> task.exercises.count { PersonalQuestion.questionIn(it, task) != null } }
        }
        assertTrue(count(setOf("1A")) >= MIN_IN_FIRST_CHAPTER)
        assertTrue(count(Chapters.all.map { it.id }.toSet()) >= MIN_IN_BOOK)
    }

    private companion object {
        const val MIN_IN_FIRST_CHAPTER = 30
        const val MIN_IN_BOOK = 60
    }
}
