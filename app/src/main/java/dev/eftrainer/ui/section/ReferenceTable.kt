package dev.eftrainer.ui.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.Panel
import dev.eftrainer.core.ui.PanelHeader
import dev.eftrainer.data.content.Reference
import dev.eftrainer.ui.exercise.inputs.AudioBar

@Composable
fun TableBlock(
    table: Reference.Table,
    modifier: Modifier = Modifier,
) {
    Panel(modifier) {
        PanelHeader(table.title)
        table.audio?.let { Box(Modifier.padding(bottom = 8.dp)) { AudioBar(it) } }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            when (tableForm(table.headers.size, maxWidth.value, fontScale)) {
                TableForm.GRID -> GridTable(table)
                TableForm.CARDS -> CardTable(table)
            }
        }
    }
}

private fun cellStyle(column: Int): TextStyle =
    if (column == 1) T.rowEn.copy(color = C.Accent, fontWeight = FontWeight.SemiBold) else T.rowEn

@Composable
private fun GridTable(table: Reference.Table) {
    DividedRows {
        table.rows.forEach { row ->
            row.forEachIndexed { c, v ->
                Text(v, style = cellStyle(c), modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardTable(table: Reference.Table) {
    Column {
        table.rows.forEachIndexed { r, row ->
            val line = if (r > 0) Modifier.drawBehind { divider() } else Modifier
            Column(line.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (row.first().isNotBlank()) {
                    Text(row.first(), style = T.rowEn.copy(fontWeight = FontWeight.Bold))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.Bottom,
                ) {
                    row.forEachIndexed { c, v ->
                        if (c > 0 && v.isNotBlank()) LabeledCell(table.headers.getOrElse(c) { "" }, v, c)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledCell(
    label: String,
    value: String,
    column: Int,
) {
    Column {
        if (label.isNotBlank()) Text(label, style = T.rowSub)
        Text(value, style = cellStyle(column))
    }
}

private fun DrawScope.divider(y: Float = 0f) {
    drawLine(EF.Teal.copy(alpha = .25f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

@Composable
private fun DividedRows(content: @Composable () -> Unit) {
    val lines = remember { mutableListOf<Int>() }
    Layout(
        content = content,
        modifier = Modifier.fillMaxWidth().drawBehind { lines.forEach { divider(it.toFloat()) } },
    ) { measurables, constraints ->
        val gap = COLUMN_GAP_DP.dp.roundToPx()
        val rows = measurables.chunked(2)
        val intrinsic = IntArray(2) { c -> rows.maxOf { it.getOrNull(c).widest() } }
        val space = constraints.maxWidth - gap
        val first = firstColumnWidth(intrinsic[0], intrinsic[1], space, MIN_COLUMN_DP.dp.roundToPx())
        val widths = intArrayOf(first, space - first)
        val placed =
            rows.map { row ->
                row.mapIndexed { c, m -> m.measure(constraints.copy(minWidth = 0, maxWidth = widths[c])) }
            }
        val heights = placed.map { row -> row.maxOf { it.height } }
        lines.clear()
        heights
            .runningFold(0) { y, h -> y + h }
            .drop(1)
            .dropLast(1)
            .forEach { lines += it }
        layout(constraints.maxWidth, heights.sum()) {
            var top = 0
            placed.forEachIndexed { r, row ->
                row.forEachIndexed { c, p -> p.placeRelative(if (c == 0) 0 else first + gap, top) }
                top += heights[r]
            }
        }
    }
}

private fun Measurable?.widest(): Int = this?.maxIntrinsicWidth(Int.MAX_VALUE) ?: 0
