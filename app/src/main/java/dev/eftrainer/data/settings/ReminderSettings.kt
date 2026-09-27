package dev.eftrainer.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

class ReminderSettings(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(ENABLED, true))
    private val _time = MutableStateFlow(LocalTime.ofSecondOfDay(prefs.getInt(TIME, DEFAULT_TIME).toLong()))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    val time: StateFlow<LocalTime> = _time.asStateFlow()

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(ENABLED, on) }
        _enabled.value = on
    }

    fun setTime(time: LocalTime) {
        prefs.edit { putInt(TIME, time.toSecondOfDay()) }
        _time.value = time
    }

    private companion object {
        const val ENABLED = "reminder"
        const val TIME = "reminder_time"
        const val DEFAULT_TIME = 19 * 60 * 60
    }
}
