package dev.eftrainer.data.content

import dev.eftrainer.domain.check.Checker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrregularVerbsContentTest {
    private val verbs = IrregularVerbs.list
    private val blanks = IrregularVerbs.exercises.filterIsInstance<Blank>()

    private fun blank(base: String) = blanks.first { it.id == IrregularVerbs.ID_PREFIX + base }

    @Test fun `whole list from the book in its order`() {
        assertEquals(68, verbs.size)
        assertEquals("be", verbs.first().base)
        assertEquals("write", verbs.last().base)
        assertEquals(verbs.map { it.base }.sorted(), verbs.map { it.base })
    }

    @Test fun `ids are stable and unique`() {
        assertEquals(verbs.map { "iv-" + it.base }, IrregularVerbs.exercises.map { it.id })
        val chapterExercises =
            Chapters.all.flatMap { c ->
                c.sections.flatMap { s -> s.tasks.flatMap { t -> t.exercises } }
            }
        assertTrue(chapterExercises.none { it.id.startsWith(IrregularVerbs.ID_PREFIX) })
        assertEquals(
            verbs.size,
            IrregularVerbs.exercises
                .map { it.id }
                .toSet()
                .size,
        )
    }

    @Test fun `every verb asks for both forms except can`() {
        for (b in blanks) {
            val expected = if (b.id == "iv-can") 1 else 2
            assertEquals(b.id, expected, b.gaps.size)
            assertTrue(b.id, b.gaps.flatten().all { it.isNotBlank() })
        }
    }

    @Test fun `first answers pass their own check`() {
        for (b in blanks) assertTrue(b.id, Checker.check(b, b.gaps.map { it.first() }).correct)
    }

    @Test fun `book variants are accepted`() {
        assertTrue(Checker.check(blank("learn"), listOf("learned", "learnt")).correct)
        assertTrue(Checker.check(blank("dream"), listOf("dreamed", "dreamed")).correct)
        assertTrue(Checker.check(blank("be"), listOf("were", "been")).correct)
        assertTrue(Checker.check(blank("go"), listOf("Went", "gone")).correct)
    }

    @Test fun `both forms must be right`() {
        assertFalse(Checker.check(blank("go"), listOf("went", "went")).correct)
        assertFalse(Checker.check(blank("go"), listOf("goed", "gone")).correct)
    }

    @Test fun `the correct answer shows just the forms`() {
        assertEquals("go – went – gone", Checker.check(blank("go"), listOf("", "")).expected)
        assertEquals("can – could", Checker.check(blank("can"), listOf("")).expected)
    }

    @Test fun `verbs stay out of chapters`() {
        assertTrue(IrregularVerbs.chapter !in Chapters.all)
        assertEquals(IrregularVerbs.section, Chapters.locate("iv-go").section)
    }
}
