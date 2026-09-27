package dev.eftrainer.ui.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.KindHeader
import dev.eftrainer.core.ui.NotePanel
import dev.eftrainer.core.ui.Panel
import dev.eftrainer.core.ui.PanelHeader
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Reference
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.SectionKind
import dev.eftrainer.ui.exercise.inputs.AudioBar
import dev.eftrainer.ui.exercise.inputs.SayIcon
import dev.eftrainer.ui.exercise.inputs.rememberVoice
import dev.eftrainer.ui.explain.ExplainHost
import dev.eftrainer.ui.explain.Selectable
import dev.eftrainer.ui.explain.explainContext
import dev.eftrainer.ui.label

@Composable
fun RulesScreen(
    sectionId: String,
    startTasks: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val section = Chapters.section(sectionId)
    val blocks = remember(sectionId) { groupMistakes(section.reference) }
    val tts = if (section.kind == SectionKind.VOCABULARY) rememberVoice() else null

    ExplainHost(remember(sectionId) { explainContext(sectionId) }) {
        Column(modifier.fillMaxSize()) {
            BackLink(stringResource(R.string.back), onBack)

            Selectable(Modifier.weight(1f)) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    RulesTitle(section, Chapters.chapterOfSection(sectionId).id)
                    blocks.forEach { ReferenceBlock(it, tts) }
                }
            }

            if (startTasks) {
                BottomBar { PrimaryButton(stringResource(R.string.go_to_tasks), onClick = onStart) }
            }
        }
    }
}

private fun groupMistakes(reference: List<Reference>): List<List<Reference>> =
    reference.fold(mutableListOf()) { acc, r ->
        if (r is Reference.Mistake && acc.lastOrNull()?.last() is Reference.Mistake) {
            acc[acc.lastIndex] = acc.last() + r
        } else {
            acc += listOf(r)
        }
        acc
    }

@Composable
private fun RulesTitle(
    section: Section,
    chapterId: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val kind =
            if (section.kind == SectionKind.GRAMMAR) {
                stringResource(R.string.ef_grammar_bank, chapterId)
            } else {
                section.kind.label()
            }
        KindHeader(section.kind, kind)
        Text(section.title, style = T.titleEn)
    }
}

@Composable
private fun ReferenceBlock(
    block: List<Reference>,
    tts: TextToSpeechState?,
) {
    when (val ref = block.first()) {
        is Reference.Note -> NotePanel(ref.title) { Text(ref.text, style = T.body) }
        is Reference.Table -> TableBlock(ref, tts)
        is Reference.Mistake -> MistakesBlock(block.filterIsInstance<Reference.Mistake>())
    }
}

@Composable
private fun TableBlock(
    table: Reference.Table,
    tts: TextToSpeechState?,
) {
    val cell = T.rowEn
    Panel {
        PanelHeader(table.title)
        table.audio?.let { Box(Modifier.padding(bottom = 8.dp)) { AudioBar(it) } }
        GridTable(columns = table.headers.size) {
            table.rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, v ->
                    val text = @Composable { m: Modifier ->
                        Text(
                            v,
                            style = if (c == 1) cell.copy(color = C.Accent, fontWeight = FontWeight.SemiBold) else cell,
                            modifier = m.padding(vertical = 8.dp),
                        )
                    }
                    if (tts != null && tts.available && c == row.lastIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            text(Modifier.weight(1f))
                            SayIcon(tts, row.joinToString(" "), "vocab-${table.title}-$r", size = 18.dp)
                        }
                    } else {
                        text(Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridTable(
    columns: Int,
    content: @Composable () -> Unit,
) {
    val lines = remember { mutableListOf<Int>() }
    val divider = EF.Teal.copy(alpha = .25f)
    Layout(
        content = content,
        modifier =
            Modifier.fillMaxWidth().drawBehind {
                lines.forEach { y ->
                    drawLine(
                        divider,
                        Offset(0f, y.toFloat()),
                        Offset(size.width, y.toFloat()),
                        1.dp.toPx(),
                    )
                }
            },
    ) { measurables, constraints ->
        val gap = 14.dp.roundToPx()
        val rows = measurables.chunked(columns)
        val widths = IntArray(columns) { c -> rows.maxOf { it.getOrNull(c)?.maxIntrinsicWidth(Int.MAX_VALUE) ?: 0 } }
        val fixed = (0 until columns - 1).sumOf { widths[it] } + gap * (columns - 1)
        widths[columns - 1] = (constraints.maxWidth - fixed).coerceAtLeast(48.dp.roundToPx())
        val placed =
            rows.map { row ->
                row.mapIndexed { c, m ->
                    m.measure(
                        constraints.copy(minWidth = 0, maxWidth = widths[c]),
                    )
                }
            }
        val heights = placed.map { row -> row.maxOf { it.height } }
        lines.clear()
        var y = 0
        heights.forEachIndexed { i, h ->
            if (i > 0) lines += y
            y += h
        }
        layout(constraints.maxWidth, y) {
            var top = 0
            placed.forEachIndexed { r, row ->
                var x = 0
                row.forEachIndexed { c, p ->
                    p.placeRelative(x, top)
                    x += widths[c] + gap
                }
                top += heights[r]
            }
        }
    }
}

@Composable
private fun MistakesBlock(mistakes: List<Reference.Mistake>) {
    Panel(alt = true) {
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            mistakes.forEach { m ->
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(color = C.Bad, textDecoration = TextDecoration.LineThrough),
                            ) { append(m.wrong) }
                            append(" → ")
                            withStyle(SpanStyle(color = C.Ok)) { append(m.right) }
                        },
                        style = T.rowEn.copy(lineHeight = 27.sp),
                    )
                    m.why?.let { Text(it, style = T.label.copy(fontSize = 14.sp, lineHeight = 20.sp)) }
                }
            }
        }
    }
}
