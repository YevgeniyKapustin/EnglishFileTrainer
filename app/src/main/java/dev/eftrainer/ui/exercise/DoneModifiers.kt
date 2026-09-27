package dev.eftrainer.ui.exercise

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.ui.BandInk
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val BUBBLE_START_SCALE = .5f
private const val BUBBLE_FADE_SPEED = 3f
private const val CHAPTER_ID_START_SCALE = .3f
private const val FLAME_BUMP = .3f
private const val FLAME_STRETCH = 1.3f
private const val FLAME_FLICKERS = 3f
private const val FLAME_TILT_DEGREES = 10f
private const val EMPHASIS_BUMP = .18f
private const val BURST_RAYS = 12
private const val BURST_TURN_DEGREES = 15f
private const val BURST_INNER_FROM = .7f
private const val BURST_INNER_SPREAD = .7f
private const val BURST_LENGTH = .45f
private const val FULL_TURN_DEGREES = 360f
private const val HALF = .5f
private val RiseDistance = 12.dp
private val BurstStroke = 4.dp

internal fun Modifier.riseIn(shown: Progress): Modifier =
    graphicsLayer {
        val value = shown.value
        alpha = value.coerceIn(0f, 1f)
        translationY = (1f - value) * RiseDistance.toPx()
    }

internal fun Modifier.popIn(
    pop: Progress,
    origin: TransformOrigin = TransformOrigin.Center,
    startScale: Float = BUBBLE_START_SCALE,
): Modifier =
    graphicsLayer {
        val value = pop.value
        val scale = startScale + (1f - startScale) * value
        scaleX = scale
        scaleY = scale
        transformOrigin = origin
        alpha = (value * BUBBLE_FADE_SPEED).coerceIn(0f, 1f)
    }

internal fun Modifier.chapterIdPop(pop: Progress): Modifier =
    popIn(pop, TransformOrigin(0f, 1f), CHAPTER_ID_START_SCALE)

internal fun Modifier.flameFlicker(flame: Progress): Modifier =
    graphicsLayer {
        val value = flame.value
        val bump = sin(PI * value).toFloat()
        scaleX = 1f + FLAME_BUMP * bump
        scaleY = 1f + FLAME_BUMP * FLAME_STRETCH * bump
        rotationZ = sin(2 * PI * FLAME_FLICKERS * value).toFloat() * FLAME_TILT_DEGREES * (1f - value)
        transformOrigin = TransformOrigin(HALF, 1f)
    }

internal fun Modifier.growEmphasis(emphasis: Progress): Modifier =
    graphicsLayer {
        val scale = 1f + EMPHASIS_BUMP * sin(PI * emphasis.value).toFloat()
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(1f, HALF)
    }

internal fun Modifier.burstRays(burst: Progress): Modifier =
    drawBehind {
        val value = burst.value
        if (value <= 0f || value >= 1f) return@drawBehind
        val reach = max(size.width, size.height) / 2
        val inner = reach * (BURST_INNER_FROM + BURST_INNER_SPREAD * value)
        val outer = inner + reach * BURST_LENGTH * (1f - value)
        val stroke = BurstStroke.toPx()
        repeat(BURST_RAYS) { i ->
            val angle = Math.toRadians((BURST_TURN_DEGREES + i * FULL_TURN_DEGREES / BURST_RAYS).toDouble())
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                BandInk,
                center + direction * inner,
                center + direction * outer,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
                alpha = 1f - value,
            )
        }
    }
