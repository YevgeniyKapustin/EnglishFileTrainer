package dev.eftrainer.ui.exercise.inputs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.eftrainer.R
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.AssetImage
import dev.eftrainer.core.ui.IconButton24
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.LocalEdgeScroll
import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Picture
import dev.eftrainer.domain.check.moved
import dev.eftrainer.ui.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private val PictureRowHeight = 88.dp
private val TextRowHeight = 116.dp
private val RowGap = 8.dp
private const val TEXT_CARD_LINES = 5
private const val SLIDE_MS = 180
private val RowShape = RoundedCornerShape(12.dp)

@Composable
fun OrderInput(
    exercise: Order,
    labels: List<String>,
    onReorder: (List<String>) -> Unit,
    checked: Boolean,
    wrongPositions: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val pictures = remember(exercise.id) { exercise.pictures.associateBy { it.label } }
    val sounds = appContainer().sounds
    val rowHeight = if (exercise.pictures.all { it.image == null }) TextRowHeight else PictureRowHeight
    val step = with(LocalDensity.current) { (rowHeight + RowGap).toPx() }
    val scope = rememberCoroutineScope()
    val drag = remember(step) { RowDrag(step, scope) }
    val reorder by rememberUpdatedState<(List<String>) -> Unit>({ order ->
        drag.slideAside(labels, order)
        onReorder(order)
        sounds.play(Cue.TICK)
    })
    val dragBy = rememberDragBy(drag, labels, reorder)
    FollowEdges(drag, dragBy)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(RowGap)) {
        labels.forEachIndexed { position, label ->
            key(label) {
                val lifted = label == drag.label
                OrderRow(
                    position = position,
                    picture = pictures.getValue(label),
                    height = rowHeight,
                    last = position == labels.lastIndex,
                    checked = checked,
                    wrong = position in wrongPositions,
                    lifted = lifted,
                    onMove = { to -> reorder(labels.moved(position, to)) },
                    modifier = Modifier.liftable(label, drag),
                    gripModifier = if (checked) Modifier else Modifier.dragHandle(label, drag) { dragBy(label, it) },
                )
            }
        }
    }
}

private fun Modifier.liftable(
    label: String,
    drag: RowDrag,
): Modifier =
    zIndex(if (label == drag.label || label == drag.settling) 1f else 0f)
        .graphicsLayer { translationY = drag.translation(label) }

@Stable
private class RowDrag(
    private val step: Float,
    private val scope: CoroutineScope,
) {
    var label by mutableStateOf<String?>(null)
    var offset by mutableFloatStateOf(0f)
    var settling by mutableStateOf<String?>(null)
    private val slides = HashMap<String, Animatable<Float, AnimationVector1D>>()
    var fingerY: Float? = null
    val grips = HashMap<String, LayoutCoordinates>()

    fun translation(row: String): Float = if (row == label) offset else slides[row]?.value ?: 0f

    fun slideAside(
        before: List<String>,
        after: List<String>,
    ) {
        after.forEachIndexed { index, row ->
            val was = before.indexOf(row)
            if (row != label && was != index) glide(row, (was - index) * step)
        }
    }

    fun drop() {
        val row = label ?: return
        val from = offset
        label = null
        offset = 0f
        settling = row
        glide(row, from) { if (settling == row) settling = null }
    }

    private fun glide(
        row: String,
        by: Float,
        done: () -> Unit = {},
    ) {
        val slide = slides.getOrPut(row) { Animatable(0f) }
        val from = slide.value + by
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            slide.snapTo(from)
            slide.animateTo(0f, tween(SLIDE_MS, easing = EaseOutCubic))
            done()
        }
    }

    fun follow(
        label: String,
        at: Offset,
    ) {
        fingerY = grips[label]?.takeIf { it.isAttached }?.localToWindow(at)?.y
    }

    fun target(
        index: Int,
        dy: Float,
        lastIndex: Int,
    ): Int? {
        offset += dy
        val target =
            when {
                offset > step / 2 && index < lastIndex -> index + 1
                offset < -step / 2 && index > 0 -> index - 1
                else -> return null
            }
        offset -= (target - index) * step
        return target
    }
}

@Composable
private fun rememberDragBy(
    drag: RowDrag,
    labels: List<String>,
    reorder: (List<String>) -> Unit,
): (String, Float) -> Unit {
    val current by rememberUpdatedState(labels)
    val onReorder by rememberUpdatedState(reorder)
    return remember(drag) {
        { label, dy ->
            val index = current.indexOf(label)
            drag.target(index, dy, current.lastIndex)?.let { onReorder(current.moved(index, it)) }
        }
    }
}

@Composable
private fun FollowEdges(
    drag: RowDrag,
    dragBy: (String, Float) -> Unit,
) {
    val edge = LocalEdgeScroll.current ?: return
    val lifted = drag.label ?: return
    val onScrolled by rememberUpdatedState(dragBy)
    LaunchedEffect(lifted, edge) {
        edge.follow({ drag.fingerY }) { used -> onScrolled(lifted, used) }
    }
}

private fun Modifier.dragHandle(
    label: String,
    drag: RowDrag,
    onDrag: (Float) -> Unit,
): Modifier =
    onGloballyPositioned { drag.grips[label] = it }
        .pointerInput(label) {
            detectDragGestures(
                onDragStart = {
                    drag.label = label
                    drag.offset = 0f
                    drag.follow(label, it)
                },
                onDragEnd = drag::drop,
                onDragCancel = drag::drop,
                onDrag = { change, amount ->
                    change.consume()
                    onDrag(amount.y)
                    drag.follow(label, change.position)
                },
            )
        }

@Composable
private fun OrderRow(
    position: Int,
    picture: Picture,
    height: Dp,
    last: Boolean,
    checked: Boolean,
    wrong: Boolean,
    lifted: Boolean,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    gripModifier: Modifier = Modifier,
) {
    val border =
        when {
            checked -> fieldColor(checked = true, wrong = wrong)
            lifted -> C.Accent
            else -> C.Line
        }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .shadow(if (lifted) 8.dp else 0.dp, RowShape)
            .clip(RowShape)
            .background(C.Field)
            .border(if (checked) 1.5.dp else 1.dp, border, RowShape)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${position + 1}",
            style =
                T.label.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) border else C.Muted,
                ),
            modifier = Modifier.width(20.dp),
        )
        if (picture.image == null) {
            CardText(picture, Modifier.weight(1f))
        } else {
            AssetImage(
                picture.image,
                picture.caption,
                Modifier.size(width = 108.dp, height = height - 16.dp).clip(RoundedCornerShape(8.dp)),
                keepRatio = false,
            )
            Text(picture.label, style = T.rowEn, modifier = Modifier.padding(start = 12.dp))
            Spacer(Modifier.weight(1f))
        }
        if (!checked) {
            Column {
                MoveButton(Icons.ChevronUp, stringResource(R.string.move_up), position > 0) { onMove(position - 1) }
                MoveButton(Icons.ChevronDown, stringResource(R.string.move_down), !last) { onMove(position + 1) }
            }
            Box(gripModifier.size(width = 40.dp, height = height), contentAlignment = Alignment.Center) {
                Icon(Icons.Grip, null, Modifier.size(20.dp), tint = C.Chevron)
            }
        }
    }
}

@Composable
private fun CardText(
    picture: Picture,
    modifier: Modifier = Modifier,
) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = EF.Title)) { append(picture.label) }
            append("  ")
            append(picture.caption)
        },
        style = T.rowEn.copy(fontSize = 15.sp, lineHeight = 20.sp),
        maxLines = TEXT_CARD_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 4.dp, end = 4.dp),
    )
}

@Composable
private fun MoveButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton24(
        icon,
        description,
        onClick = { if (enabled) onClick() },
        modifier = Modifier.size(width = 40.dp, height = 36.dp),
        tint = if (enabled) C.Muted else C.Disabled,
        size = 20,
    )
}
