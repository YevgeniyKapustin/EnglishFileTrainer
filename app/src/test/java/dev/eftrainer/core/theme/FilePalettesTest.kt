package dev.eftrainer.core.theme

import org.junit.Assert.assertSame
import org.junit.Test

class FilePalettesTest {
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
