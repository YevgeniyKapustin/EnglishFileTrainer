package dev.eftrainer.ui.current

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.FilePalette
import dev.eftrainer.core.theme.FilePalettes
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BandInk
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.EfIcons
import dev.eftrainer.core.ui.IconButton24
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.core.ui.SectionTag
import dev.eftrainer.core.ui.SpeechBubbleFit
import dev.eftrainer.core.ui.SpeechBubbleShape
import dev.eftrainer.core.ui.SpeechBubbleText
import dev.eftrainer.core.ui.WedgeBand
import dev.eftrainer.core.ui.rememberSpeechBubbleFit
import dev.eftrainer.data.content.Bubbles
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.label
import dev.eftrainer.ui.toPercent

@Composable
fun CurrentChapterScreen(
    onAllChapters: () -> Unit,
    onSection: (sectionId: String) -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    onStats: () -> Unit = {},
    chapterId: String? = null,
    vm: CurrentChapterViewModel = currentChapterViewModel(chapterId),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val state = ui ?: return

    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ChapterBand(state.chapter, !state.past, onAllChapters, onSettings) {
                if (!state.past) StreakChip(state.streak, onStats)
            }
            ChapterSummary(state)
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 28.dp)) {
                state.sections.forEachIndexed { i, row ->
                    SectionItem(row, divider = i != state.sections.lastIndex, onSection = onSection)
                }
            }
        }

        if (!state.past) ReviewBar(state, onReview)
    }
}

@Composable
private fun currentChapterViewModel(chapterId: String?): CurrentChapterViewModel =
    appContainer().let { container ->
        viewModel(key = "chapter-$chapterId") { CurrentChapterViewModel(container, chapterId) }
    }

@Composable
private fun ChapterSummary(state: CurrentUi) {
    val chapter = state.chapter
    Column(
        Modifier.padding(horizontal = 24.dp).padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (chapter == null) {
            Text(stringResource(R.string.book_finished), style = T.title)
        } else {
            Text(chapter.title, style = T.title.copy(color = EF.Title, fontWeight = FontWeight.SemiBold))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(state.mastered.toFloat(), Modifier.weight(1f))
                Text(
                    stringResource(R.string.percent, state.mastered.toPercent()),
                    style = T.label.copy(fontSize = 15.sp),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionItem(
    row: SectionRow,
    divider: Boolean,
    onSection: (sectionId: String) -> Unit,
) {
    ListRow(onClick = { onSection(row.section.id) }, divider = divider) {
        SectionTag(row.section.kind)
        Column(Modifier.weight(1f)) {
            Text(row.section.title, style = T.rowEn.copy(color = C.Ink))
            Text(
                row.section.kind.label(),
                style = T.rowSub.copy(color = C.Muted),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            stringResource(R.string.percent, row.mastered.toPercent()),
            style = T.label.copy(fontSize = 15.sp),
        )
    }
}

@Composable
private fun ChapterBand(
    chapter: Chapter?,
    intro: Boolean,
    onAllChapters: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val motion = rememberHeaderIntro(play = intro)
    WedgeBand(BandHeight, Modifier.clipToBounds(), chapter?.id, reveal = { motion.rays.value }) {
        chapter?.bubbles?.let { b -> BandBubbles(b, FilePalettes.forChapter(chapter.id), motion) }
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .heightIn(
                        min = 44.dp,
                    ).clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAllChapters)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.all_chapters), style = T.label.copy(fontSize = 15.sp, color = BandInk))
                Icon(Icons.ChevronRight, null, Modifier.size(18.dp), tint = BandInk)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(PillShape).background(IconShade).padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
                IconButton24(EfIcons.Gear, stringResource(R.string.settings), onSettings, tint = BandInk)
            }
        }
        if (chapter != null) {
            Text(
                chapter.id,
                style =
                    TextStyle(
                        fontFamily = Golos,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp,
                        color = BandInk,
                    ),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 14.dp),
            )
        }
    }
}

private val BandHeight = 150.dp
private val IconShade = Color.Black.copy(alpha = .22f)
private val PillShape = RoundedCornerShape(percent = 50)
private val QuestionMinSize = 92.dp
private val AnswerMinSize = 88.dp
private val QuestionTop = 46.dp
private val AnswerTop = 38.dp
private val AnswerEnd = 40.dp
private val BubbleOverlap = 20.dp
private val LowestBubbleBottom = 2.dp

private class BandBubble(
    val text: String,
    val color: Color,
    val fit: SpeechBubbleFit,
    val end: Dp,
    top: Dp,
    val motion: BubbleMotion,
) {
    val bottom = (BandHeight - top - fit.size).coerceAtLeast(LowestBubbleBottom)
}

@Composable
private fun BoxScope.BandBubbles(
    lines: Bubbles,
    palette: FilePalette,
    intro: HeaderIntro,
) {
    val first = rememberBubbleMotion(intro.firstBubble, tailLeft = true)
    val second = rememberBubbleMotion(intro.secondBubble, tailLeft = false)
    val question = rememberSpeechBubbleFit(lines.first, QuestionMinSize)
    val answer = rememberSpeechBubbleFit(lines.second, AnswerMinSize)
    val bubbles =
        listOf(
            BandBubble(
                lines.first,
                palette.question,
                question,
                AnswerEnd + answer.size - BubbleOverlap,
                QuestionTop,
                first,
            ),
            BandBubble(lines.second, palette.answer, answer, AnswerEnd, AnswerTop, second),
        )
    val overlap = palette.bubbleOverlap
    val overlapInk = lerp(overlap, Color.Black, .6f)

    fun BandBubble.placed() =
        Modifier
            .align(Alignment.BottomEnd)
            .padding(end = end, bottom = bottom)

    Box(Modifier.matchParentSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        bubbles.forEach { bubble ->
            SpeechBubbleShape(
                bubble.color,
                bubble.motion.tailLeft,
                bubble.fit.size,
                bubble.placed().bubbleShapeMotion(bubble.motion),
                blendMode = BlendMode.Multiply,
            )
        }
    }
    bubbles.forEach { bubble ->
        val under = if (bubble.motion === first) second else first
        SpeechBubbleText(
            bubble.text,
            bubble.fit,
            bubble
                .placed()
                .bubbleTextMotion(bubble.motion)
                .bubbleGrip(bubble.motion)
                .bubbleInk(bubble.motion, under, overlapInk),
            textModifier = Modifier.onPlaced(bubble.motion.text::place),
            onTextLayout = bubble.motion.text::layout,
        )
    }
}

@Composable
private fun StreakChip(
    streak: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Flame,
            stringResource(R.string.stats),
            Modifier.size(20.dp),
            tint =
                if (streak >
                    0
                ) {
                    EF.File2
                } else {
                    C.Off
                },
        )
        Text(
            streak.toString(),
            style = T.label.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BandInk),
        )
    }
}
