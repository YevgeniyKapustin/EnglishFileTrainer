package dev.eftrainer.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.ui.appContainer

private const val INTRO_MILLIS = 1500
private const val CELL_START_SCALE = .3f
private val RiseBy = 14.dp

private object StatsIntroGate {
    var played = false
}

@Stable
class StatsIntro(
    played: Boolean,
) {
    val fresh = !played
    val clock = Animatable(if (played) 1f else 0f)

    fun share(
        start: Float,
        end: Float,
        easing: Easing = FastOutSlowInEasing,
    ): Float = easing.transform(((clock.value - start) / (end - start)).coerceIn(0f, 1f))
}

@Composable
fun rememberStatsIntro(): StatsIntro {
    val intro = remember { StatsIntro(StatsIntroGate.played) }
    val sounds = appContainer().sounds
    LaunchedEffect(intro) {
        if (!intro.fresh) return@LaunchedEffect
        StatsIntroGate.played = true
        sounds.play(Cue.FAN)
        intro.clock.animateTo(1f, tween(INTRO_MILLIS, easing = LinearEasing))
    }
    return intro
}

fun Modifier.rise(
    intro: StatsIntro,
    start: Float,
    end: Float,
): Modifier =
    graphicsLayer {
        val t = intro.share(start, end)
        alpha = t
        translationY = (1f - t) * RiseBy.toPx()
    }

fun Modifier.popIn(
    intro: StatsIntro,
    start: Float,
    end: Float,
): Modifier =
    graphicsLayer {
        val t = intro.share(start, end, EaseOutBack)
        alpha = t.coerceIn(0f, 1f)
        scaleX = t
        scaleY = t
    }

fun Modifier.cellIn(
    intro: StatsIntro,
    start: Float,
    end: Float,
): Modifier =
    graphicsLayer {
        val t = intro.share(start, end, EaseOutBack)
        alpha = t.coerceIn(0f, 1f)
        val scale = CELL_START_SCALE + (1f - CELL_START_SCALE) * t
        scaleX = scale
        scaleY = scale
    }
