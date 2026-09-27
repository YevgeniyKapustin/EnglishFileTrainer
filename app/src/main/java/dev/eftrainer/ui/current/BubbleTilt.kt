package dev.eftrainer.ui.current

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import dev.eftrainer.core.ui.SPEECH_BUBBLE_RADIUS
import dev.eftrainer.core.ui.SpeechBubbleCenter
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun restingTiltDegrees(tip: Offset): Float {
    val reach = tip - SpeechBubbleCenter
    val lean = acos(SPEECH_BUBBLE_RADIUS / hypot(reach.x, reach.y)) - atan2(abs(reach.x), reach.y)
    val degrees = Math.toDegrees(lean.toDouble()).toFloat()
    return if (reach.x < 0f) degrees else -degrees
}

fun floorContact(tiltDegrees: Float) = SpeechBubbleCenter + Offset(0f, SPEECH_BUBBLE_RADIUS).rotated(-tiltDegrees)

fun tiltDegrees(
    pressedDown: Float,
    room: Float,
    fullTilt: Float,
) = (pressedDown / room).coerceIn(0f, 1f) * fullTilt

fun Offset.scaledFrom(
    pivot: Offset,
    scale: Offset,
) = Offset((x - pivot.x) * scale.x, (y - pivot.y) * scale.y)

fun Offset.rotated(degrees: Float): Offset {
    val turn = Math.toRadians(degrees.toDouble())
    val cosine = cos(turn).toFloat()
    val sine = sin(turn).toFloat()
    return Offset(x * cosine - y * sine, x * sine + y * cosine)
}

fun Offset.pivotShift(degrees: Float) = this - rotated(degrees)

fun wallPush(
    center: Offset,
    radii: Offset,
    degrees: Float,
    band: Size,
): Offset {
    if (band == Size.Zero) return Offset.Zero
    val turn = Math.toRadians(degrees.toDouble())
    val cosine = abs(cos(turn).toFloat())
    val sine = abs(sin(turn).toFloat())
    val half = Offset(hypot(radii.x * cosine, radii.y * sine), hypot(radii.x * sine, radii.y * cosine))
    return Offset(keptInside(center.x, half.x, band.width), keptInside(center.y, half.y, band.height))
}

private fun keptInside(
    center: Float,
    half: Float,
    span: Float,
) = when {
    center + half > span -> span - center - half
    center - half < 0f -> half - center
    else -> 0f
}
