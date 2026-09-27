package dev.eftrainer.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.ai.detectProvider
import dev.eftrainer.data.settings.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val theme = container.theme.mode
    val key = container.ai.key
    val voice = container.voice
    val sounds = container.soundSettings.enabled
    val reminder = container.reminder.enabled
    val reminderTime = container.reminder.time

    var checking by mutableStateOf(false)
        private set

    var error by mutableStateOf<AiException.Reason?>(null)
        private set

    var keyInput by mutableStateOf(
        container.ai.key.value
            .orEmpty(),
    )
        private set

    private var verifying: Job? = null

    fun setTheme(mode: ThemeMode) = container.theme.set(mode)

    fun setSounds(on: Boolean) = container.soundSettings.setEnabled(on)

    fun setReminder(on: Boolean) {
        container.reminder.setEnabled(on)
        container.reschedule()
    }

    fun setReminderTime(time: LocalTime) {
        container.reminder.setTime(time)
        container.reschedule()
    }

    fun editKey(value: String) {
        keyInput = value
        error = null
        verifying?.cancel()
        val typed = value.trim()
        when {
            typed.isEmpty() -> container.ai.setKey(null)
            detectProvider(typed) != null -> saveAndVerify(typed)
        }
    }

    private fun saveAndVerify(typed: String) {
        container.ai.setKey(typed)
        verifying =
            viewModelScope.launch {
                delay(VERIFY_DELAY_MS)
                checking = true
                try {
                    container.aiClient.verify(typed)
                } catch (e: AiException) {
                    error = e.reason
                } finally {
                    checking = false
                }
            }
    }

    fun remove() {
        verifying?.cancel()
        keyInput = ""
        error = null
        container.ai.setKey(null)
    }

    private companion object {
        const val VERIFY_DELAY_MS = 500L
    }
}
