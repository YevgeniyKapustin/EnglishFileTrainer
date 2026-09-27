package dev.eftrainer.ui.current

import androidx.compose.ui.geometry.Offset
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

fun tiltDegrees(
    pressedDown: Float,
    room: Float,
    fullTilt: Float,
) = (pressedDown / room).coerceIn(0f, 1f) * fullTilt

fun Offset.scaledFrom(
    pivot: Offset,
    scale: Offset,
) = Offset((x - pivot.x) * scale.x, (y - pivot.y) * scale.y)

fun Offset.pivotShift(degrees: Float): Offset {
    val turn = Math.toRadians(degrees.toDouble())
    val cosine = cos(turn).toFloat()
    val sine = sin(turn).toFloat()
    return Offset(x - (x * cosine - y * sine), y - (x * sine + y * cosine))
}
