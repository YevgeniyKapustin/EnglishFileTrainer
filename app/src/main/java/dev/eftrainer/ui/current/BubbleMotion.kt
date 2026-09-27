package dev.eftrainer.ui.current

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.eftrainer.core.ui.SPEECH_BUBBLE_RADIUS
import dev.eftrainer.core.ui.SpeechBubbleCenter
import dev.eftrainer.core.ui.SpeechBubbleVisibleBounds
import dev.eftrainer.core.ui.lineRects
import dev.eftrainer.core.ui.speechBubblePath
import dev.eftrainer.core.ui.speechBubbleTailTip
import kotlin.math.abs

private const val BUBBLE_START_SCALE = .6f
private const val MAX_SQUASH = .12f
private const val SQUASH_SPREAD = .5f
private const val SQUASH_FIT_STEPS = 8
private val TextMargin = 4.dp
val BubbleSquashRoom = 10.dp

class BubblePose(
    val scaleX: Float,
    val scaleY: Float,
    val origin: TransformOrigin,
    val translation: Offset,
    val rotation: Float,
)

private fun stretchOf(squeeze: Offset) =
    Offset(1f - squeeze.x + squeeze.y * SQUASH_SPREAD, 1f - squeeze.y + squeeze.x * SQUASH_SPREAD)

class BubbleTextFit {
    private var origin = Offset.Zero
    private var layout: TextLayoutResult? = null
    private var lines = emptyList<Rect>()

    fun place(text: LayoutCoordinates) {
        origin = text.positionInParent()
        lines = lineBounds()
    }

    fun layout(result: TextLayoutResult) {
        layout = result
        lines = lineBounds()
    }

    fun fits(
        box: Size,
        squeeze: Offset,
        margin: Float,
    ): Boolean {
        val stretch = stretchOf(squeeze)
        val radius = SPEECH_BUBBLE_RADIUS * box.width
        val radiusX = radius * stretch.x - margin
        val radiusY = radius * stretch.y - margin
        if (radiusX <= 0f || radiusY <= 0f) return false
        val center = SpeechBubbleCenter * box.width
        return lines.all { line ->
            listOf(line.topLeft, line.topRight, line.bottomLeft, line.bottomRight).all { corner ->
                val dx = (corner.x - center.x) / radiusX
                val dy = (corner.y - center.y) / radiusY
                dx * dx + dy * dy <= 1f
            }
        }
    }

    private fun lineBounds(): List<Rect> {
        val result = layout ?: return emptyList()
        return result.lineRects().map { it.translate(origin) }
    }
}

@Stable
class BubbleMotion(
    val pop: Animatable<Float, AnimationVector1D>,
    val tailLeft: Boolean,
) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    val squash = Animatable(Offset.Zero, Offset.VectorConverter)
    val text = BubbleTextFit()
    var travel = Rect.Zero
        private set
    var shape = Path()
        private set
    private var home = Offset.Zero
    private var boxSize = Size.Zero
    private val tip = speechBubbleTailTip(tailLeft)
    private val tailOrigin = TransformOrigin(tip.x, tip.y)
    private val fullTilt = restingTiltDegrees(tip)
    private val popScale get() = BUBBLE_START_SCALE + (1f - BUBBLE_START_SCALE) * pop.value
    private val pressed get() = Offset(squash.value.x, squash.value.y.coerceAtMost(0f))

    fun measure(bubble: LayoutCoordinates) {
        val band = bubble.parentLayoutCoordinates ?: return
        val box = bubble.size.toSize()
        home = band.localPositionOf(bubble, Offset.Zero)
        if (box != boxSize) shape = speechBubblePath(tailLeft, box)
        boxSize = box
        val visible = SpeechBubbleVisibleBounds
        travel =
            Rect(
                -home.x - visible.left * box.width,
                -home.y - visible.top * box.height,
                band.size.width - home.x - visible.right * box.width,
                band.size.height - home.y - visible.bottom * box.height,
            )
    }

    fun held(offset: Offset) =
        Offset(offset.x.coerceIn(travel.left, travel.right), offset.y.coerceIn(travel.top, travel.bottom))

    fun shapePose(density: Density): BubblePose {
        val pop = popScale
        val scale = stretchOf(fittedSqueeze(density)) * pop
        val origin = squashOrigin()
        val degrees = tilt(density)
        val reach = tipOnBox().scaledFrom(pivotOf(origin), scale)
        return BubblePose(scale.x, scale.y, origin, held(offset.value) + reach.pivotShift(degrees), degrees)
    }

    fun textPose(density: Density): BubblePose {
        val pop = popScale
        val stretch = stretchOf(fittedSqueeze(density))
        val pivot = pivotOf(squashOrigin())
        val center = SpeechBubbleCenter * boxSize.width
        val shift = Offset((center.x - pivot.x) * (stretch.x - 1f), (center.y - pivot.y) * (stretch.y - 1f)) * pop
        val degrees = tilt(density)
        val reach = pivot + tipOnBox().scaledFrom(pivot, stretch * pop) - shift - pivotOf(tailOrigin)
        return BubblePose(pop, pop, tailOrigin, held(offset.value) + shift + reach.pivotShift(degrees), degrees)
    }

    fun DrawTransform.leave(pose: BubblePose) {
        val pivot = pivotOf(pose.origin)
        translate(pivot.x, pivot.y)
        scale(1f / pose.scaleX, 1f / pose.scaleY, Offset.Zero)
        rotate(-pose.rotation, Offset.Zero)
        val placed = home + pose.translation + pivot
        translate(-placed.x, -placed.y)
    }

    fun DrawTransform.enter(pose: BubblePose) {
        val pivot = pivotOf(pose.origin)
        val placed = home + pose.translation + pivot
        translate(placed.x, placed.y)
        rotate(pose.rotation, Offset.Zero)
        scale(pose.scaleX, pose.scaleY, Offset.Zero)
        translate(-pivot.x, -pivot.y)
    }

    private fun pivotOf(origin: TransformOrigin) =
        Offset(origin.pivotFractionX * boxSize.width, origin.pivotFractionY * boxSize.height)

    private fun tipOnBox() = Offset(tip.x * boxSize.width, tip.y * boxSize.height)

    private fun tilt(density: Density) =
        tiltDegrees(squash.value.y, with(density) { BubbleSquashRoom.toPx() }, fullTilt)

    private fun squashOrigin(): TransformOrigin {
        if (pressed == Offset.Zero) return tailOrigin
        val visible = SpeechBubbleVisibleBounds
        return TransformOrigin(
            when {
                pressed.x > 0f -> visible.right
                pressed.x < 0f -> visible.left
                else -> tailOrigin.pivotFractionX
            },
            when {
                pressed.y > 0f -> visible.bottom
                pressed.y < 0f -> visible.top
                else -> tailOrigin.pivotFractionY
            },
        )
    }

    private fun fittedSqueeze(density: Density): Offset {
        if (pressed == Offset.Zero) return Offset.Zero
        val room = with(density) { BubbleSquashRoom.toPx() }
        val margin = with(density) { TextMargin.toPx() }
        val wanted =
            Offset(
                (abs(pressed.x) / room).coerceAtMost(1f) * MAX_SQUASH,
                (abs(pressed.y) / room).coerceAtMost(1f) * MAX_SQUASH,
            )
        if (text.fits(boxSize, wanted, margin)) return wanted
        var fitting = 0f
        var failing = 1f
        repeat(SQUASH_FIT_STEPS) {
            val middle = (fitting + failing) / 2
            if (text.fits(boxSize, wanted * middle, margin)) fitting = middle else failing = middle
        }
        return wanted * fitting
    }
}
