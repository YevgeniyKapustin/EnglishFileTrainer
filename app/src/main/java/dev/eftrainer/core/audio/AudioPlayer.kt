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
import androidx.compose.runtime.withFrameNanos
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

object AudioAssets {
    @Volatile private var names: Set<String>? = null

    fun candidates(track: String): List<String> {
        val base = "audio/" + track.trim().replace('.', '_')
        return listOf("$base.ogg", "$base.mp3")
    }

    fun find(
        context: Context,
        track: String,
    ): String? {
        val present = names ?: listAssets(context, "audio").also { names = it }
        return candidates(track).firstOrNull { it.removePrefix("audio/") in present }
    }
}

fun listAssets(
    context: Context,
    dir: String,
): Set<String> =
    context.assets
        .list(dir)
        ?.toSet()
        .orEmpty()

fun assetItem(
    asset: String,
    clip: Clip?,
): MediaItem =
    MediaItem
        .Builder()
        .setUri("asset:///$asset")
        .apply {
            if (clip != null) {
                setClippingConfiguration(
                    MediaItem.ClippingConfiguration
                        .Builder()
                        .setStartPositionMs(clip.startMs.toLong())
                        .setEndPositionMs(clip.endMs.toLong())
                        .build(),
                )
            }
        }.build()

@OptIn(UnstableApi::class)
fun buildPlayer(context: Context): ExoPlayer {
    val extractors = DefaultExtractorsFactory().setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
    return ExoPlayer.Builder(context, DefaultMediaSourceFactory(context, extractors)).build()
}

@Stable
class AudioPlayerState internal constructor(
    context: Context,
    val asset: String,
    clip: Clip?,
) {
    var playing by mutableStateOf(false)
        private set
    var positionMs by mutableIntStateOf(0)
        private set

    var durationMs by mutableIntStateOf(0)
        private set

    private val player: ExoPlayer =
        buildPlayer(context).apply {
            setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                false,
            )
            setMediaItem(assetItem(asset, clip))
            addListener(
                object : Player.Listener {
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
                        playing = false
                    }
                },
            )
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

    fun seekTo(ms: Int) {
        if (released || durationMs == 0) return
        val to = ms.coerceIn(0, durationMs)
        player.seekTo(to.toLong())
        positionMs = to
    }

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

        private var external: (() -> Unit)? = null

        fun pauseAll() {
            current?.pause()
            pauseExternal()
        }

        fun claimExternal(pause: () -> Unit) {
            current?.pause()
            if (external !== pause) pauseExternal()
            external = pause
        }

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

@Composable
fun rememberAudioPlayer(
    track: String?,
    clip: Clip? = null,
): AudioPlayerState? {
    val context = LocalContext.current.applicationContext
    val state =
        remember(track, clip) {
            track?.let { AudioAssets.find(context, it) }?.let { AudioPlayerState(context, it, clip) }
        }
    DisposableEffect(state) {
        state?.load()
        onDispose { state?.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { state?.pause() }
    LaunchedEffect(state, state?.playing) {
        while (state != null && state.playing) {
            withFrameNanos { state.sync() }
        }
    }
    return state
}
