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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import dev.eftrainer.core.theme.FilePalette
import dev.eftrainer.core.theme.FilePalettes

val BandInk = Color.White

private const val WEDGE_STAGGER_SPAN = .36f
private const val OPEN_RADIUS = 4f
private const val FLAP_FROM_DEGREES = -30f
private const val ACCENT_TO_DEGREES = 120f
private const val FOLDED_AT_DEGREES = 90f
private const val TEAL_EDGE = 0
private const val MAIN_EDGE = 1
private const val DEEP_EDGE = 2
private const val MID_EDGE = 3
private const val RAY_EDGE = 4
private const val LIGHT_EDGE = 5
private const val ACCENT_EDGE = 6
private val FullyRevealed = { 1f }

private class WedgePiece(
    val path: Path,
    val color: Color,
    val foldDegrees: Float,
)

private class Wedge(
    val from: Float,
    val to: Float,
    val radius: Float,
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

private fun WedgeFan.wedges(palette: FilePalette): List<Wedge> {
    val teal = edges[TEAL_EDGE]
    val main = edges[MAIN_EDGE]
    val deep = edges[DEEP_EDGE]
    val mid = edges[MID_EDGE]
    val ray = edges[RAY_EDGE]
    val light = edges[LIGHT_EDGE]
    val accent = edges[ACCENT_EDGE]
    return listOf(
        Wedge(accent, ACCENT_TO_DEGREES, OPEN_RADIUS, palette.accent),
        Wedge(light, accent, OPEN_RADIUS, palette.light),
        Wedge(ray, light, OPEN_RADIUS, palette.main),
        Wedge(mid, ray, OPEN_RADIUS, palette.mid),
        Wedge(deep, mid, deepRadius, palette.deep),
        Wedge(teal, deep, mainRadius, palette.main),
        Wedge(FLAP_FROM_DEGREES, teal, flapRadius, palette.flap),
        Wedge(teal, main, cardRadius, palette.teal),
        Wedge(FLAP_FROM_DEGREES, teal, cardRadius, palette.dark),
    )
}

private fun wedgePieces(
    fan: WedgeFan,
    palette: FilePalette,
    height: Float,
): Pair<Offset, List<WedgePiece>> {
    val pivot = Offset(fan.pivotX * height, fan.pivotY * height)
    val pieces =
        fan.wedges(palette).map { wedge ->
            val path =
                Path().apply {
                    moveTo(pivot.x, pivot.y)
                    arcTo(Rect(pivot, wedge.radius * height), wedge.from, wedge.to - wedge.from, false)
                    close()
                }
            WedgePiece(path, wedge.color, (FOLDED_AT_DEGREES - wedge.from).coerceAtLeast(0f))
        }
    return pivot to pieces
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
                val (pivot, pieces) = wedgePieces(fan, palette, size.height)
                onDrawBehind {
                    val progress = reveal()
                    clipRect {
                        pieces.forEachIndexed { i, piece ->
                            val shown = wedgeReveal(progress, i, pieces.size)
                            if (shown > 0f) {
                                rotate((1f - shown) * piece.foldDegrees, pivot) {
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
