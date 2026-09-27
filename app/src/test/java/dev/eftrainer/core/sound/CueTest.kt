package dev.eftrainer.core.sound

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun playsOnlyWhenEnabledAndRingerIsNormal() {
        assertTrue(Cue.audible(enabled = true, ringerMode = AudioManager.RINGER_MODE_NORMAL))
        assertFalse(Cue.audible(enabled = true, ringerMode = AudioManager.RINGER_MODE_VIBRATE))
        assertFalse(Cue.audible(enabled = true, ringerMode = AudioManager.RINGER_MODE_SILENT))
        assertFalse(Cue.audible(enabled = false, ringerMode = AudioManager.RINGER_MODE_NORMAL))
    }
}
