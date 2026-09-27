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

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY, on) }
        _enabled.value = on
    }

    private companion object {
        const val KEY = "ui_sounds"
    }
}
