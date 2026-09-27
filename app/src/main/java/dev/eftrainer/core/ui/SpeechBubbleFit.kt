package dev.eftrainer.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

private const val MIN_LINE_WIDTH_FRACTION = .5f
private const val FONT_SHRINK = .9f
private const val MIN_FONT_SCALE = .7f
private val TextRestMargin = 8.dp
private val MaxBubbleSize = 124.dp
private val SizeStep = 2.dp
private val LineWidthStep = 2.dp

@Immutable
class SpeechBubbleFit(
    val size: Dp,
    val lineWidth: Dp,
    val style: TextStyle,
)

fun TextLayoutResult.lineRects(): List<Rect> =
    List(lineCount) { line ->
        Rect(getLineLeft(line), getLineTop(line), getLineRight(line), getLineBottom(line))
    }

@Composable
fun rememberSpeechBubbleFit(
    text: String,
    minSize: Dp,
): SpeechBubbleFit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, minSize, measurer, density) { density.fitBubble(measurer, text, minSize) }
}

private fun TextStyle.scaled(scale: Float) = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

private fun widestLine(size: Dp) = size - 2 * SpeechBubbleTextInset

private fun Density.fitBubble(
    measurer: TextMeasurer,
    text: String,
    minSize: Dp,
): SpeechBubbleFit {
    val largest = maxOf(minSize, MaxBubbleSize)
    val sizes = generateSequence(minSize) { it + SizeStep }.takeWhile { it <= largest }
    val scales = generateSequence(1f) { it * FONT_SHRINK }.takeWhile { it >= MIN_FONT_SCALE }
    return scales.firstNotNullOfOrNull { scale ->
        val style = bubbleText.scaled(scale)
        sizes.firstNotNullOfOrNull { size -> fitLine(measurer, text, style, size) }
    } ?: SpeechBubbleFit(largest, widestLine(largest), bubbleText.scaled(MIN_FONT_SCALE))
}

private fun Density.fitLine(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    size: Dp,
): SpeechBubbleFit? {
    val narrowest = size * MIN_LINE_WIDTH_FRACTION
    return generateSequence(widestLine(size)) { it - LineWidthStep }
        .takeWhile { it >= narrowest }
        .firstOrNull { width ->
            val layout = measurer.measure(text, style, constraints = Constraints(maxWidth = width.roundToPx()))
            holds(layout, size)
        }?.let { width -> SpeechBubbleFit(size, width, style) }
}

private fun Density.holds(
    layout: TextLayoutResult,
    size: Dp,
): Boolean {
    val box = size.roundToPx().toFloat()
    val lift = SpeechBubbleTextLift.toPx()
    val origin = Offset((box - layout.size.width) / 2f, (box - lift - layout.size.height) / 2f)
    val center = SpeechBubbleCenter * box
    val radius = SPEECH_BUBBLE_RADIUS * box - TextRestMargin.toPx()
    return radius > 0f &&
        layout.lineRects().all { line ->
            val placed = line.translate(origin)
            listOf(placed.topLeft, placed.topRight, placed.bottomLeft, placed.bottomRight).all { corner ->
                (corner - center).getDistance() <= radius
            }
        }
}
