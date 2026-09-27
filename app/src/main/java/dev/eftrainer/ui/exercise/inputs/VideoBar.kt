package dev.eftrainer.ui.exercise.inputs

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.video.rememberVideoPlayer
import dev.eftrainer.data.content.Clip

@OptIn(UnstableApi::class)
@Composable
fun VideoBar(
    name: String,
    modifier: Modifier = Modifier,
    clip: Clip? = null,
) {
    val player = rememberVideoPlayer(name, clip) ?: return
    var drag by remember(player) { mutableStateOf<Float?>(null) }
    val duration = player.durationMs
    val shown = drag?.let { (it * duration).toLong() } ?: player.positionMs
    val label = stringResource(if (player.playing) R.string.video_pause else R.string.video_play)

    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(C.Field)
            .border(1.dp, C.Line, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .clickable(onClick = player::toggle),
            contentAlignment = Alignment.Center,
        ) {
            PlayerSurface(player.player, Modifier.fillMaxSize(), SURFACE_TYPE_TEXTURE_VIEW)
            if (!player.playing) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(EF.Title.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PlayIcon, label, Modifier.size(28.dp), tint = EF.TagInk)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable(onClick = player::toggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (player.playing) PauseIcon else PlayIcon, label, Modifier.size(20.dp), tint = EF.Title)
            }
            SeekBar(
                fraction = if (duration > 0) shown.toFloat() / duration else 0f,
                enabled = duration > 0,
                onDrag = { drag = it },
                onSeek = {
                    drag = null
                    player.seekTo((it * duration).toLong())
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                "${time(shown.toInt())} / ${time(duration.toInt())}",
                style = T.label.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
            )
        }
    }
}
