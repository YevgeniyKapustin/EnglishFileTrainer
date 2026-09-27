package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Picture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderCheckTest {
    private val pictures =
        Order(
            "t-pictures",
            pictures = listOf("A", "B", "C", "D").map { Picture(it, "img_${it.lowercase()}", "picture $it") },
            answer = listOf("B", "D", "C", "A"),
        )

    @Test fun `right order is correct`() {
        val result = Checker.check(pictures, listOf("B", "D", "C", "A"))
        assertTrue(result.correct)
        assertEquals(emptySet<Int>(), result.wrongGaps)
        assertEquals("B D C A", result.expected)
    }

    @Test fun `book order marks misplaced positions`() {
        val result = Checker.check(pictures, listOf("A", "B", "C", "D"))
        assertFalse(result.correct)
        assertEquals(setOf(0, 1, 3), result.wrongGaps)
    }

    @Test fun `one swap marks both positions`() {
        val result = Checker.check(pictures, listOf("B", "C", "D", "A"))
        assertFalse(result.correct)
        assertEquals(setOf(1, 2), result.wrongGaps)
    }

    @Test fun `wrong count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Checker.check(pictures, listOf("B", "D")) }
    }

    @Test fun `answer must be a permutation of the pictures`() {
        assertThrows(IllegalArgumentException::class.java) {
            pictures.copy(answer = listOf("B", "D", "C", "C"))
        }
    }

    @Test fun `moving up and down and dragging reorders the list`() {
        val start = listOf("A", "B", "C", "D")
        assertEquals(listOf("B", "A", "C", "D"), start.moved(1, 0))
        assertEquals(listOf("A", "C", "B", "D"), start.moved(1, 2))
        assertEquals(listOf("B", "C", "D", "A"), start.moved(0, 3))
        assertEquals(listOf("D", "A", "B", "C"), start.moved(3, 0))
    }

    @Test fun `moving past the ends keeps the list`() {
        val start = listOf("A", "B", "C")
        assertEquals(start, start.moved(0, -1))
        assertEquals(start, start.moved(2, 3))
        assertEquals(start, start.moved(1, 1))
    }

    @Test fun `reordering into the answer passes the check`() {
        val labels =
            listOf("A", "B", "C", "D")
                .moved(1, 0)
                .moved(3, 1)
                .moved(3, 2)
        assertEquals(listOf("B", "D", "C", "A"), labels)
        assertTrue(Checker.check(pictures, labels).correct)
    }
}
