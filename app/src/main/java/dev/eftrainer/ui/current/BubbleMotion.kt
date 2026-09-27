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
private const val TILT_ROOM_SHARE = .5f
private val TextMargin = 4.dp
val BubbleSquashRoom = 10.dp

class BubbleStance(
    val pop: Float,
    val degrees: Float,
    val held: Offset,
    val stretch: Offset,
    val pivot: Offset,
    val push: Offset,
    val textShift: Offset,
)

class BubblePose(
    val scale: Float,
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
        degrees: Float,
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
                val reach = (corner - center).rotated(degrees)
                val dx = reach.x / radiusX
                val dy = reach.y / radiusY
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
    private val onWallHit: (speed: Float) -> Unit = {},
) {
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    val squash = Animatable(Offset.Zero, Offset.VectorConverter)
    val text = BubbleTextFit()
    var travel = Rect.Zero
        private set
    var shape = Path()
        private set
    var home = Offset.Zero
        private set
    private var walls = Offset.Zero
    private var boxSize = Size.Zero
    private var bandSize = Size.Zero
    private val tip = speechBubbleTailTip(tailLeft)
    private val tailOrigin = TransformOrigin(tip.x, tip.y)
    private val fullTilt = restingTiltDegrees(tip)
    private val popScale get() = BUBBLE_START_SCALE + (1f - BUBBLE_START_SCALE) * pop.value
    private val tipOnBox get() = Offset(tip.x * boxSize.width, tip.y * boxSize.height)

    fun measure(bubble: LayoutCoordinates) {
        val band = bubble.parentLayoutCoordinates ?: return
        val box = bubble.size.toSize()
        home = band.localPositionOf(bubble, Offset.Zero)
        if (box != boxSize) shape = speechBubblePath(tailLeft, box)
        boxSize = box
        bandSize = band.size.toSize()
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

    fun touchWalls(
        reach: Offset,
        speed: Float,
    ) {
        val now = Offset(wallSide(reach.x, travel.left, travel.right), wallSide(reach.y, travel.top, travel.bottom))
        val arrived = (now.x != 0f && now.x != walls.x) || (now.y != 0f && now.y != walls.y)
        walls = now
        if (arrived) onWallHit(speed)
    }

    fun stance(density: Density): BubbleStance {
        val pop = popScale
        val room = with(density) { BubbleSquashRoom.toPx() }
        val degrees = tiltDegrees(squash.value.y, room * TILT_ROOM_SHARE, fullTilt)
        val held = held(offset.value)
        val tipOnBox = tipOnBox
        val body = held + tipOnBox + ((SpeechBubbleCenter * boxSize.width - tipOnBox) * pop).rotated(degrees)
        val stretch = stretchOf(fittedSqueeze(density, degrees))
        val pressed = squash.value
        val pivot =
            Offset(
                wallLine(pressed.x, body.x, -home.x, bandSize.width - home.x),
                wallLine(pressed.y, body.y, -home.y, bandSize.height - home.y),
            )
        val squashed = body.scaledFrom(pivot, stretch) + pivot
        val radii = stretch * (SPEECH_BUBBLE_RADIUS * boxSize.width * pop)
        val push = wallPush(home + squashed, radii, bandSize)
        return BubbleStance(pop, degrees, held, stretch, pivot, push, squashed + push - body)
    }

    fun textPose(stance: BubbleStance) =
        BubblePose(stance.pop, tailOrigin, stance.held + stance.textShift, stance.degrees)

    fun DrawTransform.placeShape(stance: BubbleStance) {
        translate(stance.push.x, stance.push.y)
        scale(stance.stretch.x, stance.stretch.y, stance.pivot)
        translate(stance.held.x, stance.held.y)
        rotate(stance.degrees, tipOnBox)
        scale(stance.pop, stance.pop, tipOnBox)
    }

    fun DrawTransform.leaveText(stance: BubbleStance) {
        scale(1f / stance.pop, 1f / stance.pop, tipOnBox)
        rotate(-stance.degrees, tipOnBox)
        val placed = stance.held + stance.textShift
        translate(-placed.x, -placed.y)
    }

    private fun fittedSqueeze(
        density: Density,
        degrees: Float,
    ): Offset {
        val pressed = squash.value
        if (pressed == Offset.Zero) return Offset.Zero
        val room = with(density) { BubbleSquashRoom.toPx() }
        val margin = with(density) { TextMargin.toPx() }
        val wanted =
            Offset(
                (abs(pressed.x) / room).coerceAtMost(1f) * MAX_SQUASH,
                (abs(pressed.y) / room).coerceAtMost(1f) * MAX_SQUASH,
            )
        if (text.fits(boxSize, wanted, degrees, margin)) return wanted
        var fitting = 0f
        var failing = 1f
        repeat(SQUASH_FIT_STEPS) {
            val middle = (fitting + failing) / 2
            if (text.fits(boxSize, wanted * middle, degrees, margin)) fitting = middle else failing = middle
        }
        return wanted * fitting
    }
}

private fun wallSide(
    at: Float,
    start: Float,
    end: Float,
) = when {
    at >= end -> 1f
    at <= start -> -1f
    else -> 0f
}
