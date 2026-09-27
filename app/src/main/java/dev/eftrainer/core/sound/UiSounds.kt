package dev.eftrainer.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dev.eftrainer.data.settings.SoundSettings
import java.util.concurrent.ConcurrentHashMap

class UiSounds(
    context: Context,
    private val settings: SoundSettings,
) {
    private val appContext = context.applicationContext
    private var pool: SoundPool? = null
    private val sampleIds = mutableMapOf<Cue, Int>()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()

    fun open() {
        if (pool != null) return
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        val created =
            SoundPool
                .Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(attributes)
                .build()
        created.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == LOAD_OK) loaded += sampleId
        }
        Cue.entries.forEach { sampleIds[it] = created.load(appContext, it.sound, 1) }
        pool = created
    }

    fun audible(): Boolean = settings.enabled.value

    fun play(
        cue: Cue,
        volume: Float = 1f,
        rate: Float = 1f,
    ): Boolean {
        val current = pool ?: return false
        val sample = sampleIds[cue]?.takeIf { it in loaded } ?: return false
        if (!audible()) return false
        return current.play(sample, volume, volume, 1, 0, rate) != 0
    }

    fun close() {
        pool?.release()
        pool = null
        sampleIds.clear()
        loaded.clear()
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val LOAD_OK = 0
    }
}
