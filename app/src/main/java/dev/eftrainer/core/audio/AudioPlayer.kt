package dev.eftrainer.core.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import dev.eftrainer.data.content.Clip
import kotlinx.coroutines.delay

/**
 * Треки Student's Book в assets: "1.9" → audio/1_9.ogg (пережатый), иначе audio/1_9.mp3.
 * Сами файлы в .gitignore — сборка без них работает, плеера просто нет.
 */
object AudioAssets {

    /** Имена-кандидаты в порядке предпочтения. Чистая функция — для тестов. */
    fun candidates(track: String): List<String> {
        val base = "audio/" + track.trim().replace('.', '_')
        return listOf("$base.ogg", "$base.mp3")
    }

    @Volatile private var names: Set<String>? = null

    /** Путь существующего файла трека или null. */
    fun find(context: Context, track: String): String? {
        val present = names ?: (context.assets.list("audio")?.toSet().orEmpty()).also { names = it }
        return candidates(track).firstOrNull { it.removePrefix("audio/") in present }
    }
}

/**
 * Файл из assets как [MediaItem]; с [clip] — только этот отрезок (Media3 ClippingConfiguration:
 * файл не режем, плеер сам играет кусок, позиция и длительность — внутри куска).
 */
fun assetItem(asset: String, clip: Clip?): MediaItem =
    MediaItem.Builder().setUri("asset:///$asset").apply {
        if (clip != null) {
            setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs.toLong())
                    .setEndPositionMs(clip.endMs.toLong())
                    .build(),
            )
        }
    }.build()

/**
 * ExoPlayer с точной перемоткой по MP3: без индекса ExoPlayer считает битрейт постоянным
 * и начало отрезка может уехать на доли секунды.
 */
@OptIn(UnstableApi::class)
fun buildPlayer(context: Context): ExoPlayer {
    val extractors = DefaultExtractorsFactory().setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
    return ExoPlayer.Builder(context, DefaultMediaSourceFactory(context, extractors)).build()
}

/**
 * Плеер трека из assets на ExoPlayer (как <audio> во фронте: состояние — поля, читаемые из Compose).
 * С [clip] играет только отрезок пункта: позиция, длительность и перемотка — внутри него.
 * Создаётся через [rememberAudioPlayer]. Одновременно играет только один плеер: старт нового ставит на паузу прежний.
 */
@Stable
class AudioPlayerState internal constructor(context: Context, val asset: String, clip: Clip?) {

    var playing by mutableStateOf(false)
        private set
    var positionMs by mutableIntStateOf(0)
        private set
    /** 0, пока файл не подготовлен. */
    var durationMs by mutableIntStateOf(0)
        private set

    private val player: ExoPlayer = buildPlayer(context).apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            false,
        )
        setMediaItem(assetItem(asset, clip))
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                sync()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) durationMs = duration.coerceAtLeast(0L).toInt()
                if (state == Player.STATE_ENDED) {
                    playWhenReady = false
                    positionMs = durationMs
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playing = false // файл битый — кнопка просто молчит
            }
        })
    }
    private var released = false

    internal fun load() {
        if (!released && player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    fun play() {
        if (released) return
        if (current !== this) current?.pause()
        pauseExternal()
        current = this
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }

    fun pause() {
        if (released) return
        player.pause()
        sync()
    }

    fun toggle() = if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) pause() else play()

    /** [ms] — от начала трека, а с отрезком — от начала отрезка; за его границы не выходит. */
    fun seekTo(ms: Int) {
        if (released || durationMs == 0) return
        val to = ms.coerceIn(0, durationMs)
        player.seekTo(to.toLong())
        positionMs = to
    }

    /** Подтянуть позицию из плеера (пока играет — несколько раз в секунду). */
    internal fun sync() {
        if (!released) positionMs = player.currentPosition.toInt()
    }

    internal fun release() {
        if (current === this) current = null
        released = true
        playing = false
        player.release()
    }

    companion object {
        private var current: AudioPlayerState? = null

        /** Другой источник звука (видео-плеер): его пауза. Играет что-то одно. */
        private var external: (() -> Unit)? = null

        /** Остановить то, что играет (например, перед записью с микрофона). */
        fun pauseAll() {
            current?.pause()
            pauseExternal()
        }

        /** Видео начинает играть: трек и прежнее видео — на паузу, [pause] запомнить. */
        fun claimExternal(pause: () -> Unit) {
            current?.pause()
            if (external !== pause) pauseExternal()
            external = pause
        }

        /** Видео-плеер уходит с экрана. */
        fun releaseExternal(pause: () -> Unit) {
            if (external === pause) external = null
        }

        private fun pauseExternal() {
            val p = external ?: return
            external = null
            p()
        }
    }
}

/**
 * Плеер трека [track] ("1.9") или null, если файла нет; с [clip] — только отрезок пункта.
 * Освобождается при уходе с экрана и смене отрезка, встаёт на паузу, когда приложение уходит в фон.
 */
@Composable
fun rememberAudioPlayer(track: String?, clip: Clip? = null): AudioPlayerState? {
    val context = LocalContext.current.applicationContext
    val state = remember(track, clip) {
        track?.let { AudioAssets.find(context, it) }?.let { AudioPlayerState(context, it, clip) }
    }
    DisposableEffect(state) {
        state?.load()
        onDispose { state?.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { state?.pause() }
    LaunchedEffect(state, state?.playing) {
        while (state != null && state.playing) {
            state.sync()
            delay(200)
        }
    }
    return state
}
