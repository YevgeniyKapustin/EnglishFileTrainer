package dev.eftrainer.ui.exercise

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DONE_RAYS_MILLIS = 520
private const val TITLE_DELAY_MILLIS = 60L
private const val TITLE_MILLIS = 320
private const val BUBBLE_DELAY_MILLIS = 200L
private const val SCORE_DELAY_MILLIS = 260L
private const val SCORE_MILLIS = 560
private const val FIRST_ROW_DELAY_MILLIS = 360L
private const val ROW_STAGGER_MILLIS = 90L
private const val ROW_MILLIS = 300
private const val FLAME_DELAY_MILLIS = 120L
private const val FLAME_MILLIS = 560
private const val PROGRESS_DELAY_MILLIS = 100L
private const val PROGRESS_MILLIS = 560
private const val EMPHASIS_MILLIS = 320
private const val CHAPTER_RAYS_MILLIS = 720
private const val CHAPTER_ID_DELAY_MILLIS = 120L
private const val BURST_DELAY_MILLIS = 260L
private const val BURST_MILLIS = 700
private const val CHAPTER_BUBBLE_DELAY_MILLIS = 460L
private const val CHAPTER_FIRST_ROW_DELAY_MILLIS = 560L
private const val DONE_ROWS = 3
private const val CHAPTER_ROWS = 2

internal typealias Progress = Animatable<Float, AnimationVector1D>

internal fun lerp(
    from: Int,
    to: Int,
    fraction: Float,
) = from + (to - from) * fraction

private fun motionPlays() = ValueAnimator.areAnimatorsEnabled()

private val popSpring get() = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)

private fun easedTween(millis: Int) = tween<Float>(millis, easing = FastOutSlowInEasing)

private fun CoroutineScope.playAfter(
    delayMillis: Long,
    block: suspend () -> Unit,
) = launch {
    delay(delayMillis)
    block()
}

private fun CoroutineScope.riseRows(
    rows: List<Progress>,
    firstDelayMillis: Long,
) = rows.forEachIndexed { i, row ->
    playAfter(firstDelayMillis + i * ROW_STAGGER_MILLIS) { row.animateTo(1f, easedTween(ROW_MILLIS)) }
}

@Stable
internal class DoneIntro(
    played: Boolean,
) {
    val pending = !played
    private val start = if (played) 1f else 0f
    val rays = Animatable(start)
    val title = Animatable(start)
    val bubble = Animatable(start)
    val score = Animatable(start)
    val rows = List(DONE_ROWS) { Animatable(start) }
    val flame = Animatable(start)
    val progress = Animatable(start)
    val emphasis = Animatable(start)
}

@Composable
internal fun rememberDoneIntro(
    rowCount: Int,
    flameRow: Int?,
    progressRow: Int?,
    grew: Boolean,
): DoneIntro {
    val intro = remember { DoneIntro(played = !motionPlays()) }
    LaunchedEffect(intro) {
        if (!intro.pending) return@LaunchedEffect
        launch { intro.rays.animateTo(1f, tween(DONE_RAYS_MILLIS, easing = LinearEasing)) }
        playAfter(TITLE_DELAY_MILLIS) { intro.title.animateTo(1f, easedTween(TITLE_MILLIS)) }
        playAfter(BUBBLE_DELAY_MILLIS) { intro.bubble.animateTo(1f, popSpring) }
        playAfter(SCORE_DELAY_MILLIS) { intro.score.animateTo(1f, easedTween(SCORE_MILLIS)) }
        riseRows(intro.rows.take(rowCount), FIRST_ROW_DELAY_MILLIS)
        intro.rows.drop(rowCount).forEach { it.snapTo(1f) }
        val rowAt = { row: Int -> FIRST_ROW_DELAY_MILLIS + row * ROW_STAGGER_MILLIS }
        if (flameRow != null) {
            playAfter(rowAt(flameRow) + FLAME_DELAY_MILLIS) {
                intro.flame.animateTo(1f, tween(FLAME_MILLIS, easing = LinearEasing))
            }
        }
        if (progressRow != null) {
            playAfter(rowAt(progressRow) + PROGRESS_DELAY_MILLIS) {
                intro.progress.animateTo(1f, easedTween(PROGRESS_MILLIS))
                if (grew) intro.emphasis.animateTo(1f, tween(EMPHASIS_MILLIS, easing = LinearEasing))
            }
        }
    }
    return intro
}

@Stable
internal class ChapterDoneIntro(
    played: Boolean,
) {
    val pending = !played
    private val start = if (played) 1f else 0f
    val rays = Animatable(start)
    val chapterId = Animatable(start)
    val burst = Animatable(start)
    val bubble = Animatable(start)
    val rows = List(CHAPTER_ROWS) { Animatable(start) }
}

@Composable
internal fun rememberChapterDoneIntro(): ChapterDoneIntro {
    val intro = remember { ChapterDoneIntro(played = !motionPlays()) }
    LaunchedEffect(intro) {
        if (!intro.pending) return@LaunchedEffect
        launch { intro.rays.animateTo(1f, tween(CHAPTER_RAYS_MILLIS, easing = LinearEasing)) }
        playAfter(CHAPTER_ID_DELAY_MILLIS) { intro.chapterId.animateTo(1f, popSpring) }
        playAfter(BURST_DELAY_MILLIS) { intro.burst.animateTo(1f, tween(BURST_MILLIS, easing = LinearEasing)) }
        playAfter(CHAPTER_BUBBLE_DELAY_MILLIS) { intro.bubble.animateTo(1f, popSpring) }
        riseRows(intro.rows, CHAPTER_FIRST_ROW_DELAY_MILLIS)
    }
    return intro
}
