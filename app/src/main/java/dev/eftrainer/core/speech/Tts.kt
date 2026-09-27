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

/**
 * Озвучка текста встроенным в телефон синтезатором (TextToSpeech): офлайн, без ключей,
 * работает с любым провайдером ИИ — озвучивается уже готовый текст. Голос en-US, если есть, иначе en-GB,
 * либо выбранный в Settings → Voice ([voiceName]).
 * Как хук во фронте: [rememberTextToSpeech] → объект с состоянием [speaking] и методами [speak] / [stop].
 * Нет синтезатора или английского голоса — [available] = false, [speak] молча ничего не делает.
 */
@Stable
class TextToSpeechState internal constructor(context: Context) {

    /** Синтезатор готов и умеет по-английски. До инициализации — false. */
    var available by mutableStateOf(false)
        private set

    /** id реплики, которая сейчас звучит, или null. */
    var speaking by mutableStateOf<String?>(null)
        private set

    /** Громкость озвучки 0..1 — применяется к каждой реплике (KEY_PARAM_VOLUME). */
    var volume = 1f
    /** Темп речи: 1 — обычный. */
    var rate = 1f

    /** Английские офлайн-голоса синтезатора — для выбора в настройках. Пусто до инициализации. */
    var voices by mutableStateOf<List<VoiceInfo>>(emptyList())
        private set

    /**
     * Выбранный голос (Voice.name) или null — «Automatic» (en-US, иначе en-GB).
     * Голоса нет или он не смог сказать — молча возвращаемся к Automatic.
     */
    var voiceName: String? = null
        set(value) {
            if (field == value) return
            field = value
            val t = tts
            if (available && t != null) applyVoice(t)
        }

    private val main = Handler(Looper.getMainLooper())
    private var pending: Pair<String, String>? = null   // speak() до готовности синтезатора
    private var released = false
    private var tts: TextToSpeech? = null
    private var locale: Locale? = null
    private var custom: String? = null                  // голос, реально поставленный через setVoice
    private var last: Pair<String, String>? = null      // для повтора другим голосом после ошибки

    init {
        tts = try {
            TextToSpeech(context.applicationContext) { status -> main.post { onInit(status) } }
        } catch (e: RuntimeException) {
            null
        }
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
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { if (!released) speaking = id } }
            override fun onDone(id: String?) { main.post { if (speaking == id) speaking = null } }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { main.post { onFailed(id) } }
            override fun onError(id: String?, errorCode: Int) { main.post { onFailed(id) } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { if (speaking == id) speaking = null } }
        })
        available = true
        pending?.let { (text, id) -> speak(text, id) }
        pending = null
    }

    private fun pickEnglish(t: TextToSpeech): Boolean = try {
        val found = LOCALES.filterNot { it in broken }.firstOrNull { l ->
            t.isLanguageAvailable(l) >= TextToSpeech.LANG_AVAILABLE &&
                t.setLanguage(l) >= TextToSpeech.LANG_AVAILABLE
        }
        locale = found
        found != null
    } catch (e: RuntimeException) {
        false
    }

    /** Пакет движка синтеза по умолчанию — для установщика голосов. */
    val engine: String? get() = try { tts?.defaultEngine } catch (e: RuntimeException) { null }

    /** Перечитать голоса (вернулись из установщика — могли появиться новые). */
    fun refreshVoices() {
        val t = tts ?: return
        if (available) voices = listVoices(t)
    }

    private fun listVoices(t: TextToSpeech): List<VoiceInfo> = try {
        t.voices.orEmpty()
            .filter { v ->
                v.locale.language == "en" && !v.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features.orEmpty()
            }
            .map { VoiceInfo(it.name, it.locale) }
    } catch (e: RuntimeException) {
        emptyList()
    }

    /** Ставит [voiceName], если он есть и не сломан; иначе — язык по умолчанию (setLanguage сбрасывает голос). */
    private fun applyVoice(t: TextToSpeech) {
        val wanted = voiceName?.takeIf { it !in brokenVoices }
        if (wanted == null) {
            if (custom != null) { custom = null; pickEnglish(t) }
            return
        }
        if (wanted == custom) return
        val ok = try {
            val v = t.voices.orEmpty().firstOrNull { it.name == wanted }
            v != null && t.setVoice(v) == TextToSpeech.SUCCESS
        } catch (e: RuntimeException) {
            false
        }
        if (ok) custom = wanted
        else if (custom != null) { custom = null; pickEnglish(t) }
    }

    /**
     * Синтезатор может заявить голос, а сказать не суметь (на TECNO en-GB скачан не до конца — ошибка -4).
     * Тогда этот голос помечаем сломанным до конца процесса и повторяем реплику следующим.
     */
    private fun onFailed(id: String?) {
        if (released) return
        val t = tts
        val retry = last?.takeIf { it.second == id }
        val failedVoice = custom
        if (t != null && failedVoice != null && retry != null) {
            // Выбранный голос не смог — дальше Automatic, до конца процесса.
            brokenVoices += failedVoice
            custom = null
            if (pickEnglish(t)) { speak(retry.first, retry.second); return }
            available = false
            if (speaking == id) speaking = null
            return
        }
        val failed = locale
        if (t != null && failed != null && retry != null) {
            broken += failed
            if (pickEnglish(t)) { speak(retry.first, retry.second); return }
            available = false
        }
        if (speaking == id) speaking = null
    }

    /** Прочитать [text] вслух, прервав прежнюю реплику. Остальное аудио приложения встаёт на паузу. */
    fun speak(text: String, id: String) {
        if (released || text.isBlank()) return
        val t = tts ?: return
        if (!available) { pending = text to id; return }
        AudioPlayerState.pauseAll()
        last = text to id
        speaking = id   // сразу, не дожидаясь onStart, — чтобы значок не мигал
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
        try { t.setSpeechRate(rate) } catch (e: RuntimeException) { /* сервис отвалился */ }
        if (t.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) speaking = null
    }

    /** Замолчать (например, перед микрофоном). */
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
            try { it.stop(); it.shutdown() } catch (e: RuntimeException) { /* сервис уже отвалился */ }
        }
        tts = null
        available = false
    }
}

private val LOCALES = listOf(Locale.US, Locale.UK)
private val broken = mutableSetOf<Locale>()
private val brokenVoices = mutableSetOf<String>()

/** Синтезатор речи для экрана: замолкает, когда приложение уходит в фон; освобождается вместе с экраном. */
@Composable
fun rememberTextToSpeech(): TextToSpeechState {
    val context = LocalContext.current
    val state = remember { TextToSpeechState(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.stop() }
    DisposableEffect(state) { onDispose { state.shutdown() } }
    return state
}
