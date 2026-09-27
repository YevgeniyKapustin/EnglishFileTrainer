package dev.eftrainer.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Темп озвучки: Slow ≈ 0.8 от обычного. */
enum class SpeechRate(val factor: Float) { NORMAL(1f), SLOW(0.8f) }

/**
 * Озвучка реплик ИИ в разговоре (plans/screens.md, п. 8 «Voice») — хранится на телефоне рядом с темой.
 * [readAloud] выключен → реплики сами не читаются, но динамик у реплики работает.
 */
class VoiceSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _readAloud = MutableStateFlow(prefs.getBoolean(KEY_READ, true))
    val readAloud: StateFlow<Boolean> = _readAloud.asStateFlow()

    /** Громкость озвучки 0..1 (поверх громкости телефона). */
    private val _volume = MutableStateFlow(prefs.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _rate = MutableStateFlow(
        prefs.getString(KEY_RATE, null)?.let { runCatching { SpeechRate.valueOf(it) }.getOrNull() } ?: SpeechRate.NORMAL,
    )
    val rate: StateFlow<SpeechRate> = _rate.asStateFlow()

    /** Выбранный голос синтезатора (Voice.name) или null — «Automatic». */
    private val _voiceName = MutableStateFlow(prefs.getString(KEY_VOICE, null))
    val voiceName: StateFlow<String?> = _voiceName.asStateFlow()

    fun setVoiceName(name: String?) {
        prefs.edit().apply { if (name == null) remove(KEY_VOICE) else putString(KEY_VOICE, name) }.apply()
        _voiceName.value = name
    }

    fun setReadAloud(on: Boolean) {
        prefs.edit().putBoolean(KEY_READ, on).apply()
        _readAloud.value = on
    }

    fun setVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_VOLUME, v).apply()
        _volume.value = v
    }

    fun setRate(rate: SpeechRate) {
        prefs.edit().putString(KEY_RATE, rate.name).apply()
        _rate.value = rate
    }

    private companion object {
        const val KEY_READ = "voice_read_aloud"
        const val KEY_VOLUME = "voice_volume"
        const val KEY_RATE = "voice_rate"
        const val KEY_VOICE = "voice_name"
    }
}
