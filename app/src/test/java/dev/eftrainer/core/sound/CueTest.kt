package dev.eftrainer.core.sound

import org.junit.Assert.assertEquals
import org.junit.Test

class CueTest {
    @Test
    fun answerPicksDingOrBonk() {
        assertEquals(Cue.CORRECT, Cue.forAnswer(correct = true))
        assertEquals(Cue.WRONG, Cue.forAnswer(correct = false))
    }

    @Test
    fun finishPicksChimeOrFanfare() {
        assertEquals(Cue.DONE, Cue.forFinish(chapterDone = false))
        assertEquals(Cue.CHAPTER, Cue.forFinish(chapterDone = true))
    }
}
