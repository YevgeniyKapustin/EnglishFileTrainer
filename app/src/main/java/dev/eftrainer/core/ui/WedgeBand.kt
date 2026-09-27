package dev.eftrainer.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import dev.eftrainer.core.theme.FilePalette
import dev.eftrainer.core.theme.FilePalettes
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val BandInk = Color.White

private const val DESIGN_WIDTH = 390f
private const val WEDGE_STAGGER_SPAN = .36f
private const val WEDGE_FOLDED_DEGREES = -32f
private const val FAN_LEFT_OVERSHOOT_DEGREES = 8f
private const val FAN_RIGHT_OVERSHOOT = 30f
private const val FLAP_START_BEFORE_TOP = 60f
private const val FLAP_TIP_BEFORE_TOP = 10f
private const val FLAP_DEPTH = 15f
private const val RAY_REACH = 2f
private val FullyRevealed = { 1f }

private class WedgePiece(
    val path: Path,
    val color: Color,
)

private fun wedgeReveal(
    reveal: Float,
    index: Int,
    count: Int,
): Float {
    if (reveal >= 1f) return 1f
    val stagger = if (count > 1) WEDGE_STAGGER_SPAN / (count - 1) else 0f
    val span = 1f - WEDGE_STAGGER_SPAN
    return FastOutSlowInEasing.transform(((reveal - index * stagger) / span).coerceIn(0f, 1f))
}

private fun triangle(
    a: Offset,
    b: Offset,
    c: Offset,
) = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    close()
}

private fun FilePalette.shade(shade: WedgeShade) =
    when (shade) {
        WedgeShade.LIGHT -> light
        WedgeShade.BRIGHT -> bright
        WedgeShade.SOFT -> soft
        WedgeShade.DEEP -> deep
        WedgeShade.DARKEST -> darkest
    }

private fun rayAngles(
    fan: WedgeFan,
    hub: Offset,
    rightEdge: Float,
): List<Float> {
    val left = atan2(hub.y, -hub.x) + Math.toRadians(FAN_LEFT_OVERSHOOT_DEGREES + fan.leftTilt.toDouble()).toFloat()
    val right = atan2(hub.y, rightEdge - hub.x)
    val total = fan.rayWeights.sum()
    return fan.rayWeights.runningFold(left) { angle, weight -> angle - (left - right) * weight / total }
}

private fun wedgePieces(
    fan: WedgeFan,
    palette: FilePalette,
    size: Size,
): List<WedgePiece> {
    val scale = size.width / DESIGN_WIDTH
    val h = size.height
    val top = fan.shieldTop * scale
    val shield =
        Path().apply {
            moveTo(0f, 0f)
            lineTo(top, 0f)
            quadraticTo(top, fan.shieldBulge * h, fan.shieldBottom * scale, h)
            lineTo(0f, h)
            close()
        }
    val flap =
        triangle(
            Offset(top - FLAP_START_BEFORE_TOP * scale, 0f),
            Offset(top + fan.flapLength * scale, 0f),
            Offset(top - FLAP_TIP_BEFORE_TOP * scale, FLAP_DEPTH * scale),
        )
    val hub = Offset(fan.hubX * scale, h + fan.hubDrop * scale)
    val reach = (size.width + h) * RAY_REACH
    val angles = rayAngles(fan, hub, top + FAN_RIGHT_OVERSHOOT * scale)

    fun far(angle: Float) = Offset(hub.x + reach * cos(angle), hub.y - reach * sin(angle))
    val rays =
        fan.rayShades.mapIndexedNotNull { i, shade ->
            shade?.let {
                val ray = triangle(hub, far(angles[i]), far(angles[i + 1]))
                WedgePiece(Path.combine(PathOperation.Intersect, ray, shield), palette.shade(it))
            }
        }
    val accent = triangle(Offset(0f, fan.accentTop * h), Offset(fan.accentWidth * scale, h), Offset(0f, h))
    return listOf(WedgePiece(flap, palette.darkest), WedgePiece(shield, palette.main)) +
        rays +
        WedgePiece(accent, palette.accent)
}

@Composable
fun WedgeBand(
    height: Dp,
    modifier: Modifier = Modifier,
    chapterId: String? = null,
    reveal: () -> Float = FullyRevealed,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val fan = remember(chapterId) { wedgeFan(chapterId) }
    val palette = FilePalettes.forChapter(chapterId)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawWithCache {
                val pieces = wedgePieces(fan, palette, size)
                onDrawBehind {
                    val progress = reveal()
                    clipRect {
                        pieces.forEachIndexed { i, piece ->
                            val shown = wedgeReveal(progress, i, pieces.size)
                            if (shown > 0f) {
                                rotate((1f - shown) * WEDGE_FOLDED_DEGREES, Offset(0f, size.height)) {
                                    drawPath(piece.path, piece.color, alpha = shown)
                                }
                            }
                        }
                    }
                }
            },
    ) {
        content()
    }
}
