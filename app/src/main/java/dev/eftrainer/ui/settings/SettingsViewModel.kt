package dev.eftrainer.ui.settings

import androidx.lifecycle.ViewModel
import dev.eftrainer.AppContainer
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.data.settings.ThemeMode
import java.time.LocalTime

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val theme = container.theme.mode
    val voice = container.voice
    val sounds = container.soundSettings.enabled
    val soundVolume = container.soundSettings.volume
    val reminder = container.reminder.enabled
    val reminderTime = container.reminder.time

    fun setTheme(mode: ThemeMode) = container.theme.set(mode)

    fun setSounds(on: Boolean) = container.soundSettings.setEnabled(on)

    fun setSoundVolume(value: Float) = container.soundSettings.setVolume(value)

    fun sampleSound() {
        container.sounds.play(Cue.CORRECT)
    }

    fun setReminder(on: Boolean) {
        container.reminder.setEnabled(on)
        container.reschedule()
    }

    fun setReminderTime(time: LocalTime) {
        container.reminder.setTime(time)
        container.reschedule()
    }
}
