package dev.eftrainer.ui.current

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
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
import dev.eftrainer.core.ui.SpeechBubble
import dev.eftrainer.core.ui.WedgeBand
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
            ChapterBand(state.chapter, onAllChapters, onSettings) {
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
private fun ReviewBar(
    state: CurrentUi,
    onReview: () -> Unit,
) {
    BottomBar {
        Text(
            reviewLine(state),
            style = T.label.copy(fontSize = 15.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
        PrimaryButton(
            stringResource(R.string.review),
            onClick = onReview,
            enabled = state.reviewCurrent + state.reviewDue > 0,
        )
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
    ListRow(onClick = { onSection(row.section.id) }.takeIf { row.open }, divider = divider) {
        SectionTag(row.section.kind, alpha = if (row.open) 1f else .4f)
        SectionItemText(row, Modifier.weight(1f))
        SectionItemStatus(row)
    }
}

@Composable
private fun SectionItemText(
    row: SectionRow,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(row.section.title, style = T.rowEn.copy(color = if (row.open) C.Ink else C.Off))
        Text(
            if (row.open) {
                row.section.kind.label()
            } else {
                stringResource(
                    R.string.opens_after,
                    row.opensAfter!!,
                )
            },
            style = T.rowSub.copy(color = if (row.open) C.Muted else C.Off),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun SectionItemStatus(row: SectionRow) {
    if (row.open) {
        Text(
            stringResource(R.string.percent, row.mastered.toPercent()),
            style = T.label.copy(fontSize = 15.sp),
        )
    } else {
        Icon(Icons.Lock, null, Modifier.size(18.dp), tint = C.Off)
    }
}

@Composable
private fun ChapterBand(
    chapter: Chapter?,
    onAllChapters: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val intro = rememberHeaderIntro()
    WedgeBand(150.dp, reveal = { intro.rays.value }) {
        chapter?.bubbles?.let { b -> BandBubbles(b, intro) }
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
            content()
            IconButton24(EfIcons.Gear, stringResource(R.string.settings), onSettings, tint = C.Muted)
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

@Composable
private fun BoxScope.BandBubbles(
    lines: Bubbles,
    intro: HeaderIntro,
) {
    SpeechBubble(
        lines.first,
        EF.Sky,
        tailLeft = true,
        size = 92.dp,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 108.dp, bottom = 12.dp)
                .bubbleMotion(intro.firstBubble, rememberBubbleDrag(), tailLeft = true),
    )
    SpeechBubble(
        lines.second,
        EF.Lime,
        tailLeft = false,
        size = 88.dp,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = 24.dp)
                .bubbleMotion(intro.secondBubble, rememberBubbleDrag(), tailLeft = false),
    )
}

@Composable
private fun StreakChip(
    streak: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
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

@Composable
private fun reviewLine(state: CurrentUi): String {
    val sections = state.reviewSections.joinToString(" and ")
    return when {
        state.reviewCurrent > 0 && state.reviewDue > 0 -> {
            pluralStringResource(R.plurals.exercises_from, state.reviewCurrent, state.reviewCurrent, sections) +
                " " +
                pluralStringResource(R.plurals.and_from_earlier, state.reviewDue, state.reviewDue)
        }

        state.reviewCurrent > 0 -> {
            pluralStringResource(
                R.plurals.exercises_from,
                state.reviewCurrent,
                state.reviewCurrent,
                sections,
            )
        }

        state.reviewDue > 0 -> {
            pluralStringResource(R.plurals.exercises_from_earlier, state.reviewDue, state.reviewDue)
        }

        else -> {
            stringResource(R.string.nothing_to_review)
        }
    }
}
