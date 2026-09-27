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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.eftrainer.core.ui.SPEECH_BUBBLE_RADIUS
import dev.eftrainer.core.ui.SpeechBubbleCenter
import dev.eftrainer.core.ui.SpeechBubbleVisibleBounds
import dev.eftrainer.core.ui.speechBubblePath
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val BUBBLE_FADE_SPEED = 3f
private const val MAX_STRETCH_FRACTION = .99f
private val DragLimit = 120.dp

@Composable
fun rememberBubbleMotion(
    pop: Animatable<Float, AnimationVector1D>,
    tailLeft: Boolean,
): BubbleMotion = remember(pop, tailLeft) { BubbleMotion(pop, tailLeft) }

private fun GraphicsLayerScope.applyPose(
    pose: BubblePose,
    pop: Float,
) {
    scaleX = pose.scaleX
    scaleY = pose.scaleY
    translationX = pose.translation.x
    translationY = pose.translation.y
    rotationZ = pose.rotation
    transformOrigin = pose.origin
    alpha = (pop * BUBBLE_FADE_SPEED).coerceIn(0f, 1f)
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

fun Modifier.bubbleShapeMotion(motion: BubbleMotion): Modifier =
    onPlaced(motion::measure).graphicsLayer { applyPose(motion.shapePose(this), motion.pop.value) }

fun Modifier.bubbleTextMotion(motion: BubbleMotion): Modifier =
    onPlaced(motion::measure).graphicsLayer { applyPose(motion.textPose(this), motion.pop.value) }

fun Modifier.bubbleGrip(motion: BubbleMotion): Modifier =
    pointerInput(motion) {
        followFinger(motion, DragLimit.toPx(), BubbleSquashRoom.toPx())
    }

fun Modifier.bubbleInk(
    own: BubbleMotion,
    under: BubbleMotion,
    ink: Color,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val ownPose = own.textPose(this)
            val underPose = under.shapePose(this)
            withTransform({
                with(own) { leave(ownPose) }
                with(under) { enter(underPose) }
            }) { drawPath(under.shape, ink, blendMode = BlendMode.SrcAtop) }
        }

private suspend fun PointerInputScope.followFinger(
    motion: BubbleMotion,
    limit: Float,
    room: Float,
) {
    val release = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    val settle = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    coroutineScope {
        var pulled = Offset.Zero
        val letGo = {
            launch { motion.offset.animateTo(Offset.Zero, release) }
            launch { motion.squash.animateTo(Offset.Zero, settle) }
        }
        detectDragGestures(
            onDragStart = {
                pulled = motion.held(motion.offset.value).unstretched(limit)
                launch { motion.offset.stop() }
            },
            onDragEnd = { letGo() },
            onDragCancel = { letGo() },
        ) { change, amount ->
            change.consume()
            val travel = motion.travel
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
            val held = motion.held(stretched)
            launch {
                motion.offset.snapTo(held)
                motion.squash.snapTo(stretched - held)
            }
        }
    }
}

private fun Float.stretched(limit: Float) = limit * this / (limit + abs(this))

private fun Float.unstretched(limit: Float) =
    limit * this / (limit - abs(this).coerceAtMost(limit * MAX_STRETCH_FRACTION))

private fun Offset.stretched(limit: Float) = Offset(x.stretched(limit), y.stretched(limit))

private fun Offset.unstretched(limit: Float) = Offset(x.unstretched(limit), y.unstretched(limit))
