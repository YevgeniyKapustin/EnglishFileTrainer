package dev.eftrainer.ui.exercise.inputs

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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Picture
import dev.eftrainer.domain.check.moved
import dev.eftrainer.ui.appContainer

private val PictureRowHeight = 88.dp
private val TextRowHeight = 116.dp
private val RowGap = 8.dp
private const val TEXT_CARD_LINES = 5
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
    val current by rememberUpdatedState(labels)
    val sounds = appContainer().sounds
    val reorder by rememberUpdatedState<(List<String>) -> Unit>({ order ->
        onReorder(order)
        sounds.play(Cue.TICK)
    })
    val rowHeight = if (exercise.pictures.all { it.image == null }) TextRowHeight else PictureRowHeight
    val step = with(LocalDensity.current) { (rowHeight + RowGap).toPx() }
    val drag = remember(step) { RowDrag(step) }

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
                    modifier = Modifier.liftable(lifted, drag),
                    gripModifier = if (checked) Modifier else Modifier.dragHandle(label, drag, { current }, reorder),
                )
            }
        }
    }
}

private fun Modifier.liftable(
    lifted: Boolean,
    drag: RowDrag,
): Modifier =
    zIndex(if (lifted) 1f else 0f)
        .graphicsLayer { translationY = if (lifted) drag.offset else 0f }

@Stable
private class RowDrag(
    private val step: Float,
) {
    var label by mutableStateOf<String?>(null)
    var offset by mutableFloatStateOf(0f)

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

private fun Modifier.dragHandle(
    label: String,
    drag: RowDrag,
    labels: () -> List<String>,
    reorder: (List<String>) -> Unit,
): Modifier =
    dragHandle(label, drag) { dy ->
        val current = labels()
        val index = current.indexOf(label)
        drag.target(index, dy, current.lastIndex)?.let { reorder(current.moved(index, it)) }
    }

private fun Modifier.dragHandle(
    label: String,
    drag: RowDrag,
    onDrag: (Float) -> Unit,
): Modifier =
    pointerInput(label) {
        detectDragGestures(
            onDragStart = {
                drag.label = label
                drag.offset = 0f
            },
            onDragEnd = { drag.label = null },
            onDragCancel = { drag.label = null },
            onDrag = { change, amount ->
                change.consume()
                onDrag(amount.y)
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
