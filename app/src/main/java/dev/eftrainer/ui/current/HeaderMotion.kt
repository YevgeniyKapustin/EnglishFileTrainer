package dev.eftrainer.ui.current

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val RAYS_MILLIS = 460
private const val FIRST_BUBBLE_DELAY_MILLIS = 220L
private const val BUBBLE_STAGGER_MILLIS = 90L
private const val BUBBLE_START_SCALE = .6f
private const val BUBBLE_FADE_SPEED = 3f
private const val DRAG_TILT_DEGREES = 10f
private const val LEFT_TAIL_PIVOT_X = .16f
private const val RIGHT_TAIL_PIVOT_X = .84f
private const val TAIL_PIVOT_Y = .96f
private const val MAX_STRETCH_FRACTION = .99f
private val DragLimit = 120.dp

@Stable
class HeaderIntro(
    played: Boolean,
) {
    private val start = if (played) 1f else 0f
    val rays = Animatable(start)
    val firstBubble = Animatable(start)
    val secondBubble = Animatable(start)
}

@Composable
fun rememberHeaderIntro(): HeaderIntro {
    var played by rememberSaveable { mutableStateOf(false) }
    val intro = remember { HeaderIntro(played) }
    LaunchedEffect(intro) {
        if (played) return@LaunchedEffect
        played = true
        val pop = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
        launch { intro.rays.animateTo(1f, tween(RAYS_MILLIS, easing = LinearEasing)) }
        delay(FIRST_BUBBLE_DELAY_MILLIS)
        launch { intro.firstBubble.animateTo(1f, pop) }
        delay(BUBBLE_STAGGER_MILLIS)
        intro.secondBubble.animateTo(1f, pop)
    }
    return intro
}

@Composable
fun rememberBubbleDrag(): Animatable<Offset, AnimationVector2D> =
    remember {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }

fun Modifier.bubbleMotion(
    pop: Animatable<Float, AnimationVector1D>,
    drag: Animatable<Offset, AnimationVector2D>,
    tailLeft: Boolean,
): Modifier =
    graphicsLayer {
        val shown = pop.value
        val scale = BUBBLE_START_SCALE + (1f - BUBBLE_START_SCALE) * shown
        val offset = drag.value
        scaleX = scale
        scaleY = scale
        alpha = (shown * BUBBLE_FADE_SPEED).coerceIn(0f, 1f)
        translationX = offset.x
        translationY = offset.y
        rotationZ = offset.x / DragLimit.toPx() * DRAG_TILT_DEGREES
        transformOrigin = TransformOrigin(if (tailLeft) LEFT_TAIL_PIVOT_X else RIGHT_TAIL_PIVOT_X, TAIL_PIVOT_Y)
    }.pointerInput(drag) {
        val limit = DragLimit.toPx()
        val release = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
        coroutineScope {
            var pulled = Offset.Zero
            detectDragGestures(
                onDragStart = {
                    pulled = drag.value.unstretched(limit)
                    launch { drag.stop() }
                },
                onDragEnd = { launch { drag.animateTo(Offset.Zero, release) } },
                onDragCancel = { launch { drag.animateTo(Offset.Zero, release) } },
            ) { change, amount ->
                change.consume()
                pulled += amount
                launch { drag.snapTo(pulled.stretched(limit)) }
            }
        }
    }

private fun Float.stretched(limit: Float) = limit * this / (limit + abs(this))

private fun Float.unstretched(limit: Float) =
    limit * this / (limit - abs(this).coerceAtMost(limit * MAX_STRETCH_FRACTION))

private fun Offset.stretched(limit: Float) = Offset(x.stretched(limit), y.stretched(limit))

private fun Offset.unstretched(limit: Float) = Offset(x.unstretched(limit), y.unstretched(limit))
