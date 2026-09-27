package dev.eftrainer.ui.exercise

import dev.eftrainer.core.sound.Cue
import dev.eftrainer.data.content.IrregularVerbs
import org.junit.Assert.assertEquals
import org.junit.Test

class VerbSpeechTest {
    private fun verb(base: String) = IrregularVerbs.list.single { it.base == base }

    @Test
    fun promptIsTheBaseForm() {
        assertEquals("go", VerbSpeech.prompt(verb("go")))
    }

    @Test
    fun formsReadBaseThenBookVariants() {
        assertEquals("go, went, gone", VerbSpeech.forms(verb("go")))
        assertEquals("dream, dreamt, dreamt", VerbSpeech.forms(verb("dream")))
        assertEquals("be, was or were, been", VerbSpeech.forms(verb("be")))
        assertEquals("can, could", VerbSpeech.forms(verb("can")))
    }

    @Test
    fun speechWaitsForTheCueToFinish() {
        assertEquals(Cue.CORRECT.millis + VerbSpeech.GAP_AFTER_CUE_MILLIS, VerbSpeech.delayAfterCheck(Cue.CORRECT))
        assertEquals(Cue.WRONG.millis + VerbSpeech.GAP_AFTER_CUE_MILLIS, VerbSpeech.delayAfterCheck(Cue.WRONG))
        assertEquals(0L, VerbSpeech.delayAfterCheck(null))
    }
}
