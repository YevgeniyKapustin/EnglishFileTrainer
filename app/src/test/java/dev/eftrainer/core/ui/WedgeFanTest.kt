package dev.eftrainer.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WedgeFanTest {
    private val lessons = (1..12).flatMap { file -> listOf("A", "B", "C").map { "$file$it" } }

    @Test
    fun withoutChapterTheFanIsTheBookOne() {
        assertSame(BookFan, wedgeFan(null))
    }

    @Test
    fun sameChapterAlwaysDrawsTheSameFan() {
        assertEquals(wedgeFan("1A"), wedgeFan("1A"))
    }

    @Test
    fun lessonsOfOneFileGetDifferentFans() {
        assertNotEquals(wedgeFan("1A"), wedgeFan("1B"))
        assertNotEquals(wedgeFan("1B"), wedgeFan("1C"))
    }

    @Test
    fun everyFanKeepsTheBookStructure() {
        lessons.map(::wedgeFan).forEach { fan ->
            assertEquals(BookFan.edges.size, fan.edges.size)
            assertEquals(fan.edges.sorted(), fan.edges)
            fan.edges.zip(BookFan.edges).forEach { (edge, book) -> assertTrue(abs(edge - book) <= EDGE_JITTER_DEGREES) }
            assertTrue(abs(fan.pivotX - BookFan.pivotX) <= PIVOT_JITTER)
            assertTrue(abs(fan.pivotY - BookFan.pivotY) <= PIVOT_JITTER)
            assertTrue(
                fan.cardRadius < fan.deepRadius && fan.deepRadius < fan.mainRadius && fan.mainRadius < fan.flapRadius,
            )
        }
    }
}
