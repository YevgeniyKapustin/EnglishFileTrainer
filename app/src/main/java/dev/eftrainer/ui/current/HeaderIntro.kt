package dev.eftrainer.ui.current

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.core.ui.SPEECH_BUBBLE_RADIUS
import dev.eftrainer.core.ui.SpeechBubbleCenter
import dev.eftrainer.core.ui.SpeechBubbleVisibleBounds
import dev.eftrainer.core.ui.speechBubblePath
import dev.eftrainer.ui.appContainer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val RAYS_MILLIS = 460
private const val FIRST_BUBBLE_DELAY_MILLIS = 220L
private const val BUBBLE_STAGGER_MILLIS = 90L

object IntroGate {
    val open = MutableStateFlow(false)
}

@Stable
class HeaderIntro(
    played: Boolean,
) {
    val pending = !played
    private val start = if (played) 1f else 0f
    val rays = Animatable(start)
    val firstBubble = Animatable(start)
    val secondBubble = Animatable(start)
}

@Composable
fun rememberHeaderIntro(): HeaderIntro {
    var played by rememberSaveable { mutableStateOf(false) }
    val intro = remember { HeaderIntro(played) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sounds = appContainer().sounds
    LaunchedEffect(intro) {
        if (!intro.pending) return@LaunchedEffect
        IntroGate.open.first { it }
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        played = true
        sounds.play(Cue.FAN)
        val pop = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
        launch { intro.rays.animateTo(1f, tween(RAYS_MILLIS, easing = LinearEasing)) }
        delay(FIRST_BUBBLE_DELAY_MILLIS)
        launch { intro.firstBubble.animateTo(1f, pop) }
        delay(BUBBLE_STAGGER_MILLIS)
        intro.secondBubble.animateTo(1f, pop)
    }
    return intro
}
