package dev.eftrainer.domain.check

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizerTest {

    private fun n(s: String) = Normalizer.normalize(s)

    @Test fun `lowercases`() = assertEquals("where do you live", n("Where Do You Live"))

    @Test fun `trims and collapses spaces`() = assertEquals("where do you live", n("  where   do\tyou  live  "))

    @Test fun `strips final punctuation`() {
        assertEquals("where were you born", n("Where were you born?"))
        assertEquals("i live in a flat", n("I live in a flat."))
        assertEquals("really", n("Really?!"))
        assertEquals("where were you born", n("Where were you born ?"))
    }

    @Test fun `keeps punctuation inside`() = assertEquals("no, i don't", n("No, I don't."))

    @Test fun `typographic apostrophe equals plain`() {
        assertEquals(n("I don't know"), n("I don’t know"))
        assertEquals(n("I don't know"), n("I don‘t know"))
    }

    @Test fun `empty stays empty`() {
        assertEquals("", n(""))
        assertEquals("", n("   "))
        assertEquals("", n("?"))
    }
}
