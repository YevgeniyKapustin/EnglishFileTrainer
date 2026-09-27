package dev.eftrainer.core.video

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.eftrainer.core.audio.AudioPlayerState
import dev.eftrainer.core.audio.assetItem
import dev.eftrainer.core.audio.buildPlayer
import dev.eftrainer.data.content.Clip
import kotlinx.coroutines.delay

/**
 * Ролики Practical English в assets: "ep1_introduction" → video/ep1_introduction.mp4.
 * Папка в .gitignore — нет файла, нет и плеера.
 */
object VideoAssets {
    fun path(name: String): String = "video/${name.trim()}.mp4"

    @Volatile private var names: Set<String>? = null

    fun exists(context: Context, name: String): Boolean {
        val present = names ?: (context.assets.list("video")?.toSet().orEmpty()).also { names = it }
        return path(name).removePrefix("video/") in present
    }
}

/**
 * Один ролик на ExoPlayer (Media3). Как [AudioPlayerState]: поля читаются из Compose;
 * старт ролика ставит на паузу трек/другой ролик, [AudioPlayerState.pauseAll] — и ролик.
 * С [clip] — только отрезок пункта (реплика), позиция и перемотка внутри него.
 */
@Stable
class VideoPlayerState internal constructor(context: Context, asset: String, clip: Clip?) {

    val player: ExoPlayer = buildPlayer(context).apply {
        setMediaItem(assetItem(asset, clip))
        prepare() // первый кадр виден сразу
    }

    var playing by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    /** 0, пока файл не подготовлен. */
    var durationMs by mutableLongStateOf(0L)
        private set

    private val pauseFromOutside: () -> Unit = { pause() }

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                sync()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) durationMs = player.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED) {
                    player.playWhenReady = false
                    positionMs = durationMs
                }
            }
        })
    }

    fun play() {
        AudioPlayerState.claimExternal(pauseFromOutside)
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }

    fun pause() {
        player.pause()
        sync()
    }

    fun toggle() = if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) pause() else play()

    fun seekTo(ms: Long) {
        val to = ms.coerceIn(0L, durationMs)
        player.seekTo(to)
        positionMs = to
    }

    internal fun sync() {
        positionMs = player.currentPosition
    }

    internal fun release() {
        AudioPlayerState.releaseExternal(pauseFromOutside)
        player.release()
    }
}

/** Плеер ролика [name] (с [clip] — отрезка) или null, если файла нет. Освобождается при уходе с экрана, пауза в фоне. */
@Composable
fun rememberVideoPlayer(name: String?, clip: Clip? = null): VideoPlayerState? {
    val context = LocalContext.current.applicationContext
    val state = remember(name, clip) {
        name?.takeIf { VideoAssets.exists(context, it) }?.let { VideoPlayerState(context, VideoAssets.path(it), clip) }
    }
    DisposableEffect(state) {
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
