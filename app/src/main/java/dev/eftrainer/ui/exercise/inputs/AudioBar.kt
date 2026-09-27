package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.audio.rememberAudioPlayer
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Clip

@Composable
fun AudioBar(
    track: String,
    modifier: Modifier = Modifier,
    clip: Clip? = null,
) {
    val player = rememberAudioPlayer(track, clip) ?: return
    var drag by remember(player) { mutableStateOf<Float?>(null) }
    val duration = player.durationMs
    val shown = drag?.let { (it * duration).toInt() } ?: player.positionMs

    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(C.Field)
            .border(1.dp, C.Line, shape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EF.Title)
                .clickable(onClick = player::toggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (player.playing) PauseIcon else PlayIcon,
                stringResource(if (player.playing) R.string.audio_pause else R.string.audio_play),
                Modifier.size(20.dp),
                tint = EF.TagInk,
            )
        }
        SeekBar(
            fraction = if (duration > 0) shown.toFloat() / duration else 0f,
            enabled = duration > 0,
            onDrag = { drag = it },
            onSeek = {
                drag = null
                player.seekTo((it * duration).toInt())
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            "${time(shown)} / ${time(duration)}",
            style = T.label.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
        )
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable { player.seekTo(player.positionMs - 5_000) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(BackIcon, stringResource(R.string.audio_back5), Modifier.size(24.dp), tint = C.Muted)
            Text(
                "5",
                style = T.label.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
internal fun SeekBar(
    fraction: Float,
    enabled: Boolean,
    onDrag: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drag by rememberUpdatedState(onDrag)
    val seek by rememberUpdatedState(onSeek)
    val accent = EF.Title
    val track = C.Line
    Canvas(
        modifier
            .height(32.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) }
            }.pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        last = (it.x / size.width).coerceIn(0f, 1f)
                        drag(last)
                    },
                    onDragEnd = { seek(last) },
                    onDragCancel = { drag(null) },
                ) { change, _ ->
                    last = (change.position.x / size.width).coerceIn(0f, 1f)
                    drag(last)
                }
            },
    ) {
        val y = size.height / 2
        val x = size.width * fraction.coerceIn(0f, 1f)
        val stroke = 4.dp.toPx()
        drawLine(track, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(accent, Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
        drawCircle(accent, 6.dp.toPx(), Offset(x, y))
    }
}

private const val MS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60

internal fun time(ms: Int): String {
    val s = ms.coerceAtLeast(0) / MS_PER_SECOND
    return "%d:%02d".format(s / SECONDS_PER_MINUTE, s % SECONDS_PER_MINUTE)
}

internal val PlayIcon = filled("M8 5.5v13l11-6.5z")
internal val PauseIcon = filled("M6.5 5h4v14h-4zM13.5 5h4v14h-4z")
internal val BackIcon = stroked("M3 4v5h5M3.6 13.5a8.5 8.5 0 1 0 2-7.5L3 9")
internal val MicIcon =
    stroked(
        "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3zM18.5 11a6.5 6.5 0 0 1-13 0M12 17.5V21",
    )
internal val SpeakerIcon = stroked("M11 5L6 9H3v6h3l5 4zM15.5 9a4 4 0 0 1 0 6M18.5 6a8 8 0 0 1 0 12")

private fun filled(d: String) =
    ImageVector
        .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            addPathNodes(d),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1f,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

private fun stroked(d: String) =
    ImageVector
        .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            addPathNodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
