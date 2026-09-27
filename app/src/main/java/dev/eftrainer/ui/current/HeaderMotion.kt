package dev.eftrainer.ui.current

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
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
private const val MAX_SQUASH = .12f
private const val SQUASH_SPREAD = .5f
private val DragLimit = 120.dp
private val SquashRoom = 10.dp

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

@Stable
class BubbleDrag {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    val squash = Animatable(Offset.Zero, Offset.VectorConverter)
    var travel = Rect.Zero
        private set

    fun measureTravel(bubble: LayoutCoordinates) {
        val band = bubble.parentLayoutCoordinates ?: return
        val home = band.localPositionOf(bubble, Offset.Zero)
        travel =
            Rect(
                -home.x,
                -home.y,
                band.size.width - home.x - bubble.size.width,
                band.size.height - home.y - bubble.size.height,
            )
    }

    fun held(offset: Offset) =
        Offset(offset.x.coerceIn(travel.left, travel.right), offset.y.coerceIn(travel.top, travel.bottom))
}

@Composable
fun rememberBubbleDrag(): BubbleDrag = remember { BubbleDrag() }

fun Modifier.bubbleMotion(
    pop: Animatable<Float, AnimationVector1D>,
    drag: BubbleDrag,
    tailLeft: Boolean,
): Modifier =
    onPlaced(drag::measureTravel)
        .graphicsLayer {
            val shown = pop.value
            val scale = BUBBLE_START_SCALE + (1f - BUBBLE_START_SCALE) * shown
            val offset = drag.held(drag.offset.value)
            val squash = drag.squash.value
            val room = SquashRoom.toPx()
            val squeezeX = (abs(squash.x) / room).coerceAtMost(1f) * MAX_SQUASH
            val squeezeY = (abs(squash.y) / room).coerceAtMost(1f) * MAX_SQUASH
            scaleX = scale * (1f - squeezeX + squeezeY * SQUASH_SPREAD)
            scaleY = scale * (1f - squeezeY + squeezeX * SQUASH_SPREAD)
            alpha = (shown * BUBBLE_FADE_SPEED).coerceIn(0f, 1f)
            translationX = offset.x
            translationY = offset.y
            rotationZ = offset.x / DragLimit.toPx() * DRAG_TILT_DEGREES
            transformOrigin = squashOrigin(squash, tailLeft)
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }

fun Modifier.bubbleGrip(drag: BubbleDrag): Modifier =
    pointerInput(drag) { followFinger(drag, DragLimit.toPx(), SquashRoom.toPx()) }

private fun squashOrigin(
    squash: Offset,
    tailLeft: Boolean,
) = TransformOrigin(
    when {
        squash.x > 0f -> 1f
        squash.x < 0f -> 0f
        tailLeft -> LEFT_TAIL_PIVOT_X
        else -> RIGHT_TAIL_PIVOT_X
    },
    when {
        squash.y > 0f -> 1f
        squash.y < 0f -> 0f
        else -> TAIL_PIVOT_Y
    },
)

private suspend fun PointerInputScope.followFinger(
    drag: BubbleDrag,
    limit: Float,
    room: Float,
) {
    val release = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    val settle = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    coroutineScope {
        var pulled = Offset.Zero
        val letGo = {
            launch { drag.offset.animateTo(Offset.Zero, release) }
            launch { drag.squash.animateTo(Offset.Zero, settle) }
        }
        detectDragGestures(
            onDragStart = {
                pulled = drag.held(drag.offset.value).unstretched(limit)
                launch { drag.offset.stop() }
            },
            onDragEnd = { letGo() },
            onDragCancel = { letGo() },
        ) { change, amount ->
            change.consume()
            val travel = drag.travel
            pulled =
                Offset(
                    (pulled.x + amount.x).coerceIn(
                        (travel.left - room).unstretched(limit),
                        (travel.right + room).unstretched(limit),
                    ),
                    (pulled.y + amount.y).coerceIn(
                        (travel.top - room).unstretched(limit),
                        (travel.bottom + room).unstretched(limit),
                    ),
                )
            val stretched = pulled.stretched(limit)
            val held = drag.held(stretched)
            launch {
                drag.offset.snapTo(held)
                drag.squash.snapTo(stretched - held)
            }
        }
    }
}

private fun Float.stretched(limit: Float) = limit * this / (limit + abs(this))

private fun Float.unstretched(limit: Float) =
    limit * this / (limit - abs(this).coerceAtMost(limit * MAX_STRETCH_FRACTION))

private fun Offset.stretched(limit: Float) = Offset(x.stretched(limit), y.stretched(limit))

private fun Offset.unstretched(limit: Float) = Offset(x.unstretched(limit), y.unstretched(limit))
