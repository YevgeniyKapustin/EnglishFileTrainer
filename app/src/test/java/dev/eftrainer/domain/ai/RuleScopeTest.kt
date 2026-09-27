package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.SectionKind
import dev.eftrainer.domain.learning.chapter
import dev.eftrainer.domain.learning.section
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleScopeTest {
    private val review = section("rv", 2).copy(kind = SectionKind.REVIEW)
    private val a = chapter("1A", section("a", 1))
    private val b = chapter("1B", section("b", 1))
    private val c = chapter("1C", section("c", 1), review)
    private val next = chapter("2A", section("d", 1))
    private val all = listOf(a, b, c, next)

    @Test fun `unit is the chapters with the same number up to this one`() {
        assertEquals(listOf("1A", "1B"), RuleScope.unitOf(all, b).map { it.id })
        assertEquals(listOf("2A"), RuleScope.unitOf(all, next).map { it.id })
    }

    @Test fun `review section draws on the lessons of the whole unit`() {
        assertEquals(listOf("a", "b", "c"), RuleScope.forExercise(all, c, review).map { it.id })
        assertEquals(listOf("a", "b", "c"), RuleScope.forChapter(all, c, review).map { it.id })
    }

    @Test fun `lesson section keeps its own rules`() {
        val lesson = c.sections.first()
        assertEquals(listOf(lesson), RuleScope.forExercise(all, c, lesson))
        assertEquals(c.sections, RuleScope.forChapter(all, c, lesson))
    }
}
