package dev.eftrainer.ui.section

import org.junit.Assert.assertEquals
import org.junit.Test

class TableFormTest {
    @Test fun `two columns stay a grid on a narrow phone`() {
        assertEquals(TableForm.GRID, tableForm(columns = 2, availableDp = 280f))
    }

    @Test fun `two columns stack when large text leaves no room`() {
        assertEquals(TableForm.CARDS, tableForm(columns = 2, availableDp = 280f, fontScale = 1.3f))
    }

    @Test fun `two columns stack below the minimum width`() {
        assertEquals(TableForm.CARDS, tableForm(columns = 2, availableDp = 250f))
    }

    @Test fun `three or more columns always stack`() {
        assertEquals(TableForm.CARDS, tableForm(columns = 3, availableDp = 600f))
        assertEquals(TableForm.CARDS, tableForm(columns = 4, availableDp = 1200f))
    }

    @Test fun `a single column is a card list`() {
        assertEquals(TableForm.CARDS, tableForm(columns = 1, availableDp = 280f))
    }

    @Test fun `first column keeps its width when both fit`() {
        assertEquals(80, firstColumnWidth(intrinsic = 80, otherIntrinsic = 150, space = 266, minColumn = 120))
    }

    @Test fun `long first column cannot squeeze the second below the minimum`() {
        assertEquals(146, firstColumnWidth(intrinsic = 400, otherIntrinsic = 100, space = 266, minColumn = 120))
    }

    @Test fun `long columns share the space in proportion`() {
        assertEquals(120, firstColumnWidth(intrinsic = 200, otherIntrinsic = 260, space = 266, minColumn = 120))
        assertEquals(133, firstColumnWidth(intrinsic = 300, otherIntrinsic = 300, space = 266, minColumn = 120))
    }
}
