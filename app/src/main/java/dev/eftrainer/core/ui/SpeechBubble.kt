package dev.eftrainer.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private const val LEFT_TAIL_START_DEGREES = 125f
private const val RIGHT_TAIL_START_DEGREES = 75f
private const val BUBBLE_ARC_DEGREES = 340f
private const val BUBBLE_ALPHA = .8f
private val LeftTailTip = Offset(.16f, .96f)
private val RightTailTip = Offset(.84f, .96f)

const val SPEECH_BUBBLE_RADIUS = .44f
val SpeechBubbleCenter = Offset(.5f, .46f)
val SpeechBubbleVisibleBounds = Rect(.06f, .02f, .94f, .96f)
val SpeechBubbleTextInset = 12.dp
val SpeechBubbleTextLift = 12.dp

fun speechBubbleTailTip(tailLeft: Boolean) = if (tailLeft) LeftTailTip else RightTailTip

fun speechBubblePath(
    tailLeft: Boolean,
    size: Size,
): Path {
    val k = size.width
    val start = if (tailLeft) LEFT_TAIL_START_DEGREES else RIGHT_TAIL_START_DEGREES
    val tip = speechBubbleTailTip(tailLeft) * k
    val rad = Math.toRadians(start.toDouble())
    val circle = Rect(SpeechBubbleCenter * k, SPEECH_BUBBLE_RADIUS * k)
    val radius = circle.width / 2
    return Path().apply {
        moveTo(circle.center.x + radius * cos(rad).toFloat(), circle.center.y + radius * sin(rad).toFloat())
        arcTo(circle, start, BUBBLE_ARC_DEGREES, false)
        lineTo(tip.x, tip.y)
        close()
    }
}

@Composable
fun SpeechBubbleShape(
    color: Color,
    tailLeft: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    Spacer(
        modifier.size(size).drawWithCache {
            val path = speechBubblePath(tailLeft, this.size)
            onDrawBehind { drawPath(path, color, alpha = BUBBLE_ALPHA, blendMode = blendMode) }
        },
    )
}

@Composable
fun SpeechBubbleText(
    text: String,
    fit: SpeechBubbleFit,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Box(modifier.size(fit.size), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = fit.style,
            onTextLayout = onTextLayout,
            modifier =
                Modifier
                    .padding(bottom = SpeechBubbleTextLift)
                    .widthIn(max = fit.lineWidth)
                    .then(textModifier),
        )
    }
}
