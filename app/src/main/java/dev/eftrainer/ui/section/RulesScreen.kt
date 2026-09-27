package dev.eftrainer.ui.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.KindHeader
import dev.eftrainer.core.ui.NotePanel
import dev.eftrainer.core.ui.Panel
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Reference
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.SectionKind
import dev.eftrainer.ui.exercise.inputs.rememberVoice
import dev.eftrainer.ui.explain.ExplainHost
import dev.eftrainer.ui.explain.Selectable
import dev.eftrainer.ui.explain.explainContext
import dev.eftrainer.ui.label

@Composable
fun RulesScreen(
    sectionId: String,
    onBack: () -> Unit,
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
private fun MistakesBlock(mistakes: List<Reference.Mistake>) {
    Panel(alt = true) {
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            mistakes.forEach { m ->
                Column {
                    MistakeLine(m)
                    m.why?.let { Text(it, style = T.label.copy(fontSize = 14.sp, lineHeight = 20.sp)) }
                }
            }
        }
    }
}

@Composable
private fun MistakeLine(mistake: Reference.Mistake) {
    val style = T.rowEn.copy(lineHeight = 27.sp)
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inline = mistakeText(mistake, " → ")
        val fits =
            remember(mistake, constraints.maxWidth, style) {
                val width = Constraints(maxWidth = constraints.maxWidth)
                measurer.measure(inline, style, constraints = width).lineCount < 2
            }
        Text(if (fits) inline else mistakeText(mistake, "\n→ "), style = style)
    }
}

private fun mistakeText(
    mistake: Reference.Mistake,
    arrow: String,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = C.Bad, textDecoration = TextDecoration.LineThrough)) { append(mistake.wrong) }
        append(arrow)
        withStyle(SpanStyle(color = C.Ok)) { append(mistake.right) }
    }
