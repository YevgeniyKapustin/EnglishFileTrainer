package dev.eftrainer.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val EdgeZone = 72.dp
private val TopSpeed = 14.dp

@Stable
class EdgeScroll(
    private val state: ScrollState,
    private val zone: Float,
    private val topSpeed: Float,
) {
    private var viewport = Rect.Zero

    fun placeViewport(bounds: Rect) {
        viewport = bounds
    }

    suspend fun follow(
        fingerY: () -> Float?,
        onScrolled: (Float) -> Unit,
    ) {
        while (true) {
            withFrameNanos { }
            val speed = fingerY()?.let(::speedAt) ?: 0f
            if (speed != 0f) {
                val used = state.scrollBy(speed)
                if (used != 0f) onScrolled(used)
            }
        }
    }

    private fun speedAt(y: Float): Float {
        val below = y - (viewport.bottom - zone)
        val above = viewport.top + zone - y
        return when {
            below > 0f -> topSpeed * (below / zone).coerceAtMost(1f)
            above > 0f -> -topSpeed * (above / zone).coerceAtMost(1f)
            else -> 0f
        }
    }
}

fun Modifier.edgeScrollViewport(edge: EdgeScroll): Modifier =
    onGloballyPositioned {
        edge.placeViewport(it.boundsInWindow())
    }

val LocalEdgeScroll = staticCompositionLocalOf<EdgeScroll?> { null }

@Composable
fun rememberEdgeScroll(state: ScrollState): EdgeScroll {
    val density = LocalDensity.current
    return remember(state, density) {
        with(density) { EdgeScroll(state, EdgeZone.toPx(), TopSpeed.toPx()) }
    }
}
