package dev.eftrainer.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BandInk
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.Bubble
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.core.ui.WedgeBand
import dev.eftrainer.core.ui.bubbleText
import dev.eftrainer.core.ui.color

@Composable
internal fun DoneScreen(
    d: SessionUi.Done,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DoneBand(d)
            Column(Modifier.padding(horizontal = 24.dp)) {
                if (d.chapterId != null) ChapterProgress(d, d.chapterId)
                if (d.mistakes.isNotEmpty()) Mistakes(d.mistakes)
            }
        }
        BottomBar { PrimaryButton(stringResource(R.string.close), onClick = onClose) }
    }
}

@Composable
private fun DoneBand(d: SessionUi.Done) {
    WedgeBand(128.dp) {
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
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 16.dp),
        )
        Bubble(100.dp, EF.Lime, Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 16.dp)) {
            Text(
                stringResource(R.string.ef_score, d.firstTry, d.total),
                style = bubbleText.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            )
            Text(stringResource(R.string.ef_right_first_time), style = bubbleText)
        }
    }
}

@Composable
private fun ChapterProgress(
    d: SessionUi.Done,
    chapterId: String,
) {
    Column {
        Text(
            stringResource(R.string.chapter_n, chapterId),
            style = T.label.copy(color = EF.Title, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 32.dp),
        )
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressBar(d.after / 100f, Modifier.weight(1f))
            Text(
                stringResource(R.string.percent_change, d.before, d.after),
                style = T.body.copy(fontSize = 15.sp),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun Mistakes(mistakes: List<String>) {
    Column {
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
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ChapterDoneBand(c)
            ChapterDoneText(c)
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
private fun ChapterDoneBand(c: SessionUi.ChapterDone) {
    WedgeBand(300.dp) {
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
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 28.dp),
        )
        c.next?.let { next ->
            Bubble(116.dp, EF.Lime, Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 30.dp)) {
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
private fun ChapterDoneText(c: SessionUi.ChapterDone) {
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
        )
        Text(
            if (c.next != null) {
                stringResource(R.string.next_is_open, c.next.id, c.chapter.id)
            } else {
                stringResource(R.string.book_complete_note, c.chapter.id)
            },
            style = T.body.copy(color = C.Muted),
        )
    }
}
