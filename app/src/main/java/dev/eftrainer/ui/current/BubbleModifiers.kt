package dev.eftrainer.ui.current

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp

private const val BUBBLE_FADE_SPEED = 3f
private val DragLimit = 120.dp

@Composable
fun rememberBubbleMotion(
    pop: Animatable<Float, AnimationVector1D>,
    tailLeft: Boolean,
): BubbleMotion = remember(pop, tailLeft) { BubbleMotion(pop, tailLeft) }

private fun GraphicsLayerScope.fadeIn(pop: Float) {
    alpha = (pop * BUBBLE_FADE_SPEED).coerceIn(0f, 1f)
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

private fun GraphicsLayerScope.applyPose(
    pose: BubblePose,
    pop: Float,
) {
    scaleX = pose.scale
    scaleY = pose.scale
    translationX = pose.translation.x
    translationY = pose.translation.y
    rotationZ = pose.rotation
    transformOrigin = pose.origin
    fadeIn(pop)
}

fun Modifier.bubbleShapeMotion(motion: BubbleMotion): Modifier =
    onPlaced(motion::measure)
        .graphicsLayer { fadeIn(motion.pop.value) }
        .drawWithContent {
            val stance = motion.stance(this)
            withTransform({ with(motion) { placeShape(stance) } }) { this@drawWithContent.drawContent() }
        }

fun Modifier.bubbleTextMotion(motion: BubbleMotion): Modifier =
    onPlaced(motion::measure).graphicsLayer { applyPose(motion.textPose(motion.stance(this)), motion.pop.value) }

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
            val ownStance = own.stance(this)
            val underStance = under.stance(this)
            val apart = under.home - own.home
            withTransform({
                with(own) { leaveText(ownStance) }
                translate(apart.x, apart.y)
                with(under) { placeShape(underStance) }
            }) { drawPath(under.shape, ink, blendMode = BlendMode.SrcAtop) }
        }
