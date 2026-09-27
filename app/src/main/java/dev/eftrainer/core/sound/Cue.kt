package dev.eftrainer.core.sound

import android.media.AudioManager
import androidx.annotation.RawRes
import dev.eftrainer.R

enum class Cue(
    @param:RawRes val sound: Int,
    val millis: Long,
) {
    CORRECT(R.raw.sound_correct, 500L),
    WRONG(R.raw.sound_wrong, 320L),
    DONE(R.raw.sound_done, 800L),
    CHAPTER(R.raw.sound_chapter, 1_300L),
    TICK(R.raw.sound_tick, 40L),
    ;

    companion object {
        fun forAnswer(correct: Boolean): Cue = if (correct) CORRECT else WRONG

        fun forFinish(chapterDone: Boolean): Cue = if (chapterDone) CHAPTER else DONE

        fun audible(
            enabled: Boolean,
            ringerMode: Int,
        ): Boolean = enabled && ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
