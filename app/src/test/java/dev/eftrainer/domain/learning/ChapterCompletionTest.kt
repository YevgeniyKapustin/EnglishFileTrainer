package dev.eftrainer.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterCompletionTest {
    private val grammar = section("g", 5)
    private val vocab = section("v", 5)
    private val pron = section("p", 0 + 2)
    private val c1 = chapter("1A", grammar, vocab, pron)
    private val c2 = chapter("1B", section("b", 3))

    @Test fun `complete once every book exercise is answered, mastered or not`() {
        val big = chapter("X", section("x", 10))
        val all = (1..10).map { progress("x-$it") }.byId()
        val nine = (1..9).map { mastered("x-$it") }.byId()
        assertTrue(ChapterCompletion.isComplete(big, all))
        assertFalse(ChapterCompletion.isComplete(big, nine))
    }

    @Test fun `current is the first not completed`() {
        assertEquals("1A", ChapterCompletion.current(listOf(c1, c2), emptySet())?.id)
        assertEquals("1B", ChapterCompletion.current(listOf(c1, c2), setOf("1A"))?.id)
        assertNull(ChapterCompletion.current(listOf(c1, c2), setOf("1A", "1B")))
    }

    @Test fun `all sections are open from the start`() {
        assertEquals(listOf("g", "v", "p"), ChapterCompletion.openSections(c1).map { it.id })
    }

    @Test fun `review covers first and started sections only`() {
        assertEquals(listOf("g"), ChapterCompletion.activeSections(c1, emptyMap()).map { it.id })
        val progress = listOf(progress("p-1")).byId()
        assertEquals(listOf("g", "p"), ChapterCompletion.activeSections(c1, progress).map { it.id })
    }
}
