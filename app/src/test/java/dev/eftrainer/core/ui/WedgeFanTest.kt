package dev.eftrainer.core.ui

import dev.eftrainer.core.theme.FilePalettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WedgeFanTest {
    @Test
    fun sameChapterAlwaysDrawsTheSameFan() {
        assertEquals(wedgeFan("1A"), wedgeFan("1A"))
        assertEquals(wedgeFan(null), wedgeFan(null))
    }

    @Test
    fun lessonsOfOneFileGetDifferentFans() {
        assertNotEquals(wedgeFan("1A"), wedgeFan("1B"))
        assertNotEquals(wedgeFan("1B"), wedgeFan("1C"))
    }

    @Test
    fun everyFanEndsWithTheDarkRaysOverTheTop() {
        listOf("1A", "1B", "2C", "12C").map(::wedgeFan).forEach { fan ->
            assertEquals(listOf(WedgeShade.DEEP, WedgeShade.DARKEST), fan.rayShades.takeLast(2))
            assertEquals(fan.rayWeights.size, fan.rayShades.size)
        }
    }

    @Test
    fun filesCycleThroughTheFourBookPalettes() {
        assertSame(FilePalettes.Lagoon, FilePalettes.forChapter("1A"))
        assertSame(FilePalettes.Amber, FilePalettes.forChapter("2B"))
        assertSame(FilePalettes.Meadow, FilePalettes.forChapter("7C"))
        assertSame(FilePalettes.Coral, FilePalettes.forChapter("12A"))
        assertSame(FilePalettes.Lagoon, FilePalettes.forChapter("9B"))
        assertSame(FilePalettes.Lagoon, FilePalettes.forChapter(null))
    }
}
