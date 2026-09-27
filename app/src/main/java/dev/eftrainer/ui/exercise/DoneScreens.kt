package dev.eftrainer.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.FilePalettes
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BandInk
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.Bubble
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.core.ui.WedgeBand
import dev.eftrainer.core.ui.bubbleText
import dev.eftrainer.core.ui.color
import kotlin.math.roundToInt

private enum class DoneRow { Streak, Progress, Mistakes }

private fun doneRows(d: SessionUi.Done) =
    listOfNotNull(
        DoneRow.Streak.takeIf { d.streak > 0 },
        DoneRow.Progress.takeIf { d.chapterId != null },
        DoneRow.Mistakes.takeIf { d.mistakes.isNotEmpty() },
    )

@Composable
internal fun DoneScreen(
    d: SessionUi.Done,
    onClose: () -> Unit,
) {
    val rows = remember(d) { doneRows(d) }
    val intro =
        rememberDoneIntro(
            rowCount = rows.size,
            flameRow = rows.indexOf(DoneRow.Streak).takeIf { it >= 0 },
            progressRow = rows.indexOf(DoneRow.Progress).takeIf { it >= 0 },
            grew = d.after > d.before,
        )
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DoneBand(d, intro)
            Column(Modifier.padding(horizontal = 24.dp)) {
                rows.forEachIndexed { i, row ->
                    val rise = Modifier.riseIn(intro.rows[i])
                    when (row) {
                        DoneRow.Streak -> StreakLine(d.streak, intro, rise)
                        DoneRow.Progress -> ChapterProgress(d, d.chapterId.orEmpty(), intro, rise)
                        DoneRow.Mistakes -> Mistakes(d.mistakes, rise)
                    }
                }
            }
        }
        BottomBar { PrimaryButton(stringResource(R.string.close), onClick = onClose) }
    }
}

@Composable
private fun DoneBand(
    d: SessionUi.Done,
    intro: DoneIntro,
) {
    WedgeBand(128.dp, chapterId = d.chapterId, reveal = { intro.rays.value }) {
        Text(
            stringResource(R.string.done),
            style =
                TextStyle(
                    fontFamily = Golos,
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = BandInk,
                ),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 16.dp)
                    .riseIn(intro.title),
        )
        Bubble(
            100.dp,
            FilePalettes.forChapter(d.chapterId).answer,
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 16.dp)
                .popIn(intro.bubble),
        ) {
            CountedScore(d, intro)
            Text(stringResource(R.string.ef_right_first_time), style = bubbleText)
        }
    }
}

@Composable
private fun CountedScore(
    d: SessionUi.Done,
    intro: DoneIntro,
) {
    val shown by remember(d.firstTry) { derivedStateOf { (intro.score.value * d.firstTry).roundToInt() } }
    Text(
        stringResource(R.string.ef_score, shown, d.total),
        style = bubbleText.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun StreakLine(
    streak: Int,
    intro: DoneIntro,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Flame, null, Modifier.size(18.dp).flameFlicker(intro.flame), tint = EF.File2)
        Text(stringResource(R.string.day_in_a_row, streak), style = T.body.copy(fontSize = 15.sp))
    }
}

@Composable
private fun ChapterProgress(
    d: SessionUi.Done,
    chapterId: String,
    intro: DoneIntro,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(R.string.chapter_n, chapterId),
            style = T.label.copy(color = EF.Title, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 32.dp),
        )
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressBar({ lerp(d.before, d.after, intro.progress.value) / 100f }, Modifier.weight(1f))
            CountedPercent(d, intro)
        }
    }
}

@Composable
private fun CountedPercent(
    d: SessionUi.Done,
    intro: DoneIntro,
) {
    val shown by remember(d.before, d.after) {
        derivedStateOf { lerp(d.before, d.after, intro.progress.value).roundToInt() }
    }
    Text(
        stringResource(R.string.percent_change, d.before, shown),
        style = T.body.copy(fontSize = 15.sp),
        modifier = Modifier.padding(start = 12.dp).growEmphasis(intro.emphasis),
    )
}

@Composable
private fun Mistakes(
    mistakes: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(R.string.mistakes_back_tomorrow),
            style = T.label.copy(color = C.Bad, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 32.dp),
        )
        mistakes.forEachIndexed { i, m ->
            ListRow(
                onClick = null,
                divider = i != mistakes.lastIndex,
            ) { Text(m, style = T.rowEn.copy(fontSize = 18.sp, lineHeight = 27.sp)) }
        }
    }
}

@Composable
internal fun ChapterDoneScreen(
    c: SessionUi.ChapterDone,
    onClose: () -> Unit,
) {
    val intro = rememberChapterDoneIntro()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ChapterDoneBand(c, intro)
            ChapterDoneText(c, intro)
        }
        BottomBar {
            PrimaryButton(
                if (c.next != null) {
                    stringResource(
                        R.string.start_chapter,
                        c.next.id,
                    )
                } else {
                    stringResource(R.string.close)
                },
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun ChapterDoneBand(
    c: SessionUi.ChapterDone,
    intro: ChapterDoneIntro,
) {
    WedgeBand(300.dp, chapterId = c.chapter.id, reveal = { intro.rays.value }) {
        Text(
            c.chapter.id,
            style =
                TextStyle(
                    fontFamily = Golos,
                    fontSize = 96.sp,
                    lineHeight = 96.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    color = BandInk,
                ),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 28.dp)
                    .burstRays(intro.burst)
                    .chapterIdPop(intro.chapterId),
        )
        c.next?.let { next ->
            Bubble(
                116.dp,
                FilePalettes.forChapter(c.chapter.id).answer,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 30.dp)
                    .popIn(intro.bubble),
            ) {
                Text(stringResource(R.string.ef_next), style = bubbleText)
                Text(
                    next.id,
                    style = bubbleText.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
                )
                Text(next.title, style = bubbleText, maxLines = 2)
            }
        }
    }
}

@Composable
private fun ChapterDoneText(
    c: SessionUi.ChapterDone,
    intro: ChapterDoneIntro,
) {
    Column(
        Modifier.padding(horizontal = 24.dp).padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.chapter_complete, c.chapter.id),
            style =
                T.title.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EF.Title,
                ),
            modifier = Modifier.riseIn(intro.rows[0]),
        )
        Text(
            if (c.next != null) {
                stringResource(R.string.next_is_open, c.next.id, c.chapter.id)
            } else {
                stringResource(R.string.book_complete_note, c.chapter.id)
            },
            style = T.body.copy(color = C.Muted),
            modifier = Modifier.riseIn(intro.rows[1]),
        )
    }
}
