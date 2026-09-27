package dev.eftrainer.core.speech

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.eftrainer.core.audio.AudioPlayerState
import java.util.Locale

@Stable
class TextToSpeechState internal constructor(
    context: Context,
) {
    var available by mutableStateOf(false)
        private set

    var speaking by mutableStateOf<String?>(null)
        private set

    var volume = 1f

    var rate = 1f

    var voices by mutableStateOf(knownVoices)
        private set

    var voiceName: String? = null
        set(value) {
            if (field == value) return
            field = value
            val t = tts
            if (available && t != null) applyVoice(t)
        }

    private val main = Handler(Looper.getMainLooper())
    private var pending: Pair<String, String>? = null
    private var released = false
    private var tts: TextToSpeech? = null
    private var locale: Locale? = null
    private var custom: String? = null
    private var last: Pair<String, String>? = null

    val engine: String? get() = runCatching { tts?.defaultEngine }.getOrNull()

    init {
        tts =
            runCatching {
                TextToSpeech(
                    context.applicationContext,
                ) { status -> main.post { onInit(status) } }
            }.getOrNull()
    }

    private fun onInit(status: Int) {
        val t = tts ?: return
        if (released) return
        if (status != TextToSpeech.SUCCESS || !pickEnglish(t)) {
            pending = null
            return
        }
        voices = listVoices(t)
        applyVoice(t)
        t.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    main.post { if (!released) speaking = id }
                }

                override fun onDone(id: String?) {
                    main.post { if (speaking == id) speaking = null }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    main.post { onFailed(id) }
                }

                override fun onError(
                    id: String?,
                    errorCode: Int,
                ) {
                    main.post { onFailed(id) }
                }

                override fun onStop(
                    id: String?,
                    interrupted: Boolean,
                ) {
                    main.post { if (speaking == id) speaking = null }
                }
            },
        )
        available = true
        pending?.let { (text, id) -> speak(text, id) }
        pending = null
    }

    private fun pickEnglish(t: TextToSpeech): Boolean =
        runCatching {
            val found =
                LOCALES.filterNot { it in broken }.firstOrNull { l ->
                    t.isLanguageAvailable(l) >= TextToSpeech.LANG_AVAILABLE &&
                        t.setLanguage(l) >= TextToSpeech.LANG_AVAILABLE
                }
            locale = found
            found != null
        }.getOrDefault(false)

    fun refreshVoices() {
        val t = tts ?: return
        if (available) voices = listVoices(t)
    }

    private fun listVoices(t: TextToSpeech): List<VoiceInfo> = englishVoices(t).also { knownVoices = it }

    private fun englishVoices(t: TextToSpeech): List<VoiceInfo> =
        runCatching {
            t.voices
                .orEmpty()
                .filter { v ->
                    v.locale.language == "en" &&
                        !v.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features.orEmpty()
                }.map { VoiceInfo(it.name, it.locale) }
        }.getOrDefault(emptyList())

    private fun applyVoice(t: TextToSpeech) {
        val wanted = voiceName?.takeIf { it !in brokenVoices }
        if (wanted == null) {
            if (custom != null) {
                custom = null
                pickEnglish(t)
            }
            return
        }
        if (wanted == custom) return
        val ok =
            runCatching {
                val v = t.voices.orEmpty().firstOrNull { it.name == wanted }
                v != null && t.setVoice(v) == TextToSpeech.SUCCESS
            }.getOrDefault(false)
        if (ok) {
            custom = wanted
        } else if (custom != null) {
            custom = null
            pickEnglish(t)
        }
    }

    private fun onFailed(id: String?) {
        if (released) return
        val t = tts
        val retry = last?.takeIf { it.second == id }
        if (t != null && retry != null && recoverFromFailure(t)) {
            speak(retry.first, retry.second)
        } else if (speaking == id) {
            speaking = null
        }
    }

    private fun recoverFromFailure(t: TextToSpeech): Boolean {
        val failedVoice = custom
        val failedLocale = locale
        if (failedVoice == null && failedLocale == null) return false
        if (failedVoice != null) {
            brokenVoices += failedVoice
            custom = null
        } else if (failedLocale != null) {
            broken += failedLocale
        }
        val recovered = pickEnglish(t)
        if (!recovered) available = false
        return recovered
    }

    fun speak(
        text: String,
        id: String,
    ) {
        if (released || text.isBlank()) return
        val t = tts ?: return
        if (!available) {
            pending = text to id
            return
        }
        AudioPlayerState.pauseAll()
        last = text to id
        speaking = id
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
        runCatching { t.setSpeechRate(rate) }
        if (t.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) speaking = null
    }

    fun stop() {
        pending = null
        speaking = null
        if (available) tts?.stop()
    }

    internal fun shutdown() {
        released = true
        pending = null
        speaking = null
        tts?.let {
            runCatching {
                it.stop()
                it.shutdown()
            }
        }
        tts = null
        available = false
    }
}

private val LOCALES = listOf(Locale.US, Locale.UK)
private val broken = mutableSetOf<Locale>()
private val brokenVoices = mutableSetOf<String>()
private var knownVoices = emptyList<VoiceInfo>()

@Composable
fun rememberTextToSpeech(): TextToSpeechState {
    val context = LocalContext.current
    val state = remember { TextToSpeechState(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.stop() }
    DisposableEffect(state) { onDispose { state.shutdown() } }
    return state
}
