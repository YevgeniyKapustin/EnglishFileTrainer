package dev.eftrainer.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BoldMarkupTest {
    @Test
    fun plainTextHasNoBold() {
        assertEquals(Marked("It started to get warmer.", emptyList()), parseBold("It started to get warmer."))
    }

    @Test
    fun markedWordsAreBoldWithoutTheMarks() {
        val marked = parseBold("One day, they **went cycling** and **ate**.")
        assertEquals("One day, they went cycling and ate.", marked.plain)
        assertEquals(listOf("went cycling", "ate"), marked.bold.map { marked.plain.substring(it) })
    }

    @Test
    fun unbalancedMarksStayAsTyped() {
        assertEquals(Marked("a **b", emptyList()), parseBold("a **b"))
    }
}
