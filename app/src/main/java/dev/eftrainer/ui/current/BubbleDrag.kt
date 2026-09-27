package dev.eftrainer.ui.current

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MAX_STRETCH_FRACTION = .99f
private const val MILLIS_PER_SECOND = 1000f

suspend fun PointerInputScope.followFinger(
    motion: BubbleMotion,
    limit: Float,
    room: Float,
) {
    val release = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    val settle = spring<Offset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    coroutineScope {
        var pulled = Offset.Zero
        val letGo = {
            launch {
                motion.offset.animateTo(Offset.Zero, release) { motion.touchWalls(value, velocity.getDistance()) }
            }
            launch { motion.squash.animateTo(Offset.Zero, settle) }
        }
        detectDragGestures(
            onDragStart = {
                pulled = motion.held(motion.offset.value).unstretched(limit)
                launch { motion.offset.stop() }
            },
            onDragEnd = { letGo() },
            onDragCancel = { letGo() },
        ) { change, amount ->
            change.consume()
            val travel = motion.travel
            pulled =
                Offset(
                    (pulled.x + amount.x).coerceIn(
                        (travel.left - room).unstretched(limit),
                        (travel.right + room).unstretched(limit),
                    ),
                    (pulled.y + amount.y).coerceIn(
                        (travel.top - room).unstretched(limit),
                        (travel.bottom + room).unstretched(limit),
                    ),
                )
            val stretched = pulled.stretched(limit)
            val held = motion.held(stretched)
            val seconds = (change.uptimeMillis - change.previousUptimeMillis).coerceAtLeast(1L) / MILLIS_PER_SECOND
            motion.touchWalls(stretched, (change.position - change.previousPosition).getDistance() / seconds)
            launch {
                motion.offset.snapTo(held)
                motion.squash.snapTo(stretched - held)
            }
        }
    }
}

private fun Float.stretched(limit: Float) = limit * this / (limit + abs(this))

private fun Float.unstretched(limit: Float) =
    limit * this / (limit - abs(this).coerceAtMost(limit * MAX_STRETCH_FRACTION))

private fun Offset.stretched(limit: Float) = Offset(x.stretched(limit), y.stretched(limit))

private fun Offset.unstretched(limit: Float) = Offset(x.unstretched(limit), y.unstretched(limit))
