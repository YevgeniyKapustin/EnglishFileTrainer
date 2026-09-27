package dev.eftrainer.core.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Почему распознавание не удалось. */
enum class SpeechError {
    /** Ничего не разобрал или не дождался речи — можно просто попробовать ещё раз. */
    NO_MATCH,
    /** Нет доступа к микрофону. */
    NO_PERMISSION,
    /** Распознавателя нет, язык не поддержан, нет сети у облачного — лучше перейти на ввод текстом. */
    UNAVAILABLE,
}

/**
 * Распознавание речи (платформенный SpeechRecognizer) как состояние для Compose.
 * Как хук во фронте: [rememberSpeechRecognizer] возвращает объект с полями-состоянием
 * и методами [start] / [stop] / [cancel].
 *
 * Типичное использование:
 * ```
 * val speech = rememberSpeechRecognizer { hypotheses -> send(hypotheses.first()) }
 * if (speech.available) MicButton(onClick = { if (speech.listening) speech.stop() else speech.start() })
 * Text(speech.partial)
 * ```
 */
@Stable
class SpeechRecognizerState internal constructor(
    private val context: Context,
    private val language: String,
    private val maxResults: Int,
) {
    /** Есть ли на телефоне сервис распознавания. false — сразу показывайте ввод текстом. */
    val available: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** Микрофон слушает (от [start] до результата или ошибки). */
    var listening by mutableStateOf(false)
        private set
    /** Промежуточный текст, пока человек говорит. */
    var partial by mutableStateOf("")
        private set
    /** Громкость голоса 0..1 — для анимации кнопки. */
    var level by mutableFloatStateOf(0f)
        private set
    /** Последняя ошибка; сбрасывается при [start]. */
    var error by mutableStateOf<SpeechError?>(null)
        internal set

    internal var onResults: (List<String>) -> Unit = {}
    internal var requestPermission: () -> Unit = {}

    private var recognizer: SpeechRecognizer? = null

    /** Начать слушать. Если нет разрешения на микрофон — сначала спросит его. */
    fun start() {
        error = null
        if (!available) { error = SpeechError.UNAVAILABLE; return }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
            return
        }
        startListening()
    }

    /** Закончить запись: результат ещё придёт. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Прервать без результата. */
    fun cancel() {
        if (!listening) return
        recognizer?.cancel()
        listening = false
        partial = ""
        level = 0f
    }

    /** Прервать и забыть ошибку и промежуточный текст — как для нового вопроса. */
    fun reset() {
        cancel()
        partial = ""
        error = null
    }

    internal fun startListening() {
        val r = recognizer ?: try {
            SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(listener)
                recognizer = it
            }
        } catch (e: RuntimeException) {
            error = SpeechError.UNAVAILABLE
            return
        }
        partial = ""
        level = 0f
        listening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        try {
            r.startListening(intent)
        } catch (e: RuntimeException) {
            listening = false
            error = SpeechError.UNAVAILABLE
        }
    }

    internal fun destroy() {
        recognizer?.destroy()
        recognizer = null
        listening = false
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { level = 0f }
        override fun onEvent(eventType: Int, params: Bundle?) {}

        // rms приходит примерно в диапазоне −2..10 дБ.
        override fun onRmsChanged(rmsdB: Float) {
            if (listening) level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (listening && !text.isNullOrBlank()) partial = text
        }

        override fun onResults(results: Bundle?) {
            if (!listening) return
            listening = false
            level = 0f
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }
            if (list.isEmpty()) error = SpeechError.NO_MATCH else onResults(list)
        }

        override fun onError(code: Int) {
            // После cancel() некоторые сервисы присылают ERROR_CLIENT — это не ошибка для нас.
            if (!listening) return
            listening = false
            level = 0f
            error = when (code) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_MATCH
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.NO_PERMISSION
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError.NO_MATCH // занят — повторить через миг
                else -> SpeechError.UNAVAILABLE // сеть, сервер, язык, клиент
            }
        }
    }
}

/**
 * Распознаватель речи для экрана. [onResults] получает до [maxResults] гипотез,
 * лучшая первой. Слушание прерывается, когда приложение уходит в фон;
 * распознаватель уничтожается вместе с экраном.
 */
@Composable
fun rememberSpeechRecognizer(
    language: String = "en-US",
    maxResults: Int = 5,
    onResults: (List<String>) -> Unit,
): SpeechRecognizerState {
    val context = LocalContext.current
    val state = remember(language, maxResults) { SpeechRecognizerState(context, language, maxResults) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) state.startListening() else state.error = SpeechError.NO_PERMISSION
    }
    SideEffect {
        state.onResults = onResults
        state.requestPermission = { permission.launch(Manifest.permission.RECORD_AUDIO) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.cancel() }
    DisposableEffect(state) { onDispose { state.destroy() } }
    return state
}
