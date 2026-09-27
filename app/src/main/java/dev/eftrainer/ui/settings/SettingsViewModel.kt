package dev.eftrainer.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.settings.ThemeMode
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val theme = container.theme.mode
    val key = container.ai.key
    val voice = container.voice

    var checking by mutableStateOf(false)
        private set

    var error by mutableStateOf<AiException.Reason?>(null)

    fun setTheme(mode: ThemeMode) = container.theme.set(mode)

    fun save(
        value: String,
        onSaved: () -> Unit,
    ) {
        if (checking) return
        checking = true
        error = null
        viewModelScope.launch {
            try {
                container.aiClient.verify(value.trim())
                container.ai.setKey(value)
                onSaved()
            } catch (e: AiException) {
                error = e.reason
            } finally {
                checking = false
            }
        }
    }

    fun remove() = container.ai.setKey(null)
}
