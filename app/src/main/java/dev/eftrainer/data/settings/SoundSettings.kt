package dev.eftrainer.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundSettings(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _volume = MutableStateFlow(prefs.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY, on) }
        _enabled.value = on
    }

    fun setVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_VOLUME, v) }
        _volume.value = v
    }

    private companion object {
        const val KEY = "ui_sounds"
        const val KEY_VOLUME = "ui_sounds_volume"
    }
}
