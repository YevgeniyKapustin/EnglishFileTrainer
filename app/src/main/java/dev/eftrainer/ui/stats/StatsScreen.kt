package dev.eftrainer.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.BandInk
import dev.eftrainer.core.ui.Bubble
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.core.ui.SectionTag
import dev.eftrainer.core.ui.WedgeBand
import dev.eftrainer.core.ui.bubbleText
import dev.eftrainer.domain.learning.Stats
import dev.eftrainer.domain.learning.WeakSpot
import dev.eftrainer.domain.learning.Week
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.toPercent
import java.util.Locale
import java.time.format.TextStyle as DateStyle

private val CELL = 14.dp
private val GAP = 4.dp
private val HEAT_SHARES = listOf(0f, .35f, .65f, 1f)

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onSection: (sectionId: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: StatsViewModel = statsViewModel(),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val state = ui ?: return

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StreakBand(state, onBack)
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 22.dp)) {
            Heatmap(state)
            state.week?.let { WeekLine(it) }
            if (state.weak.isNotEmpty()) WeakSpots(state.weak, onSection)
            Completed(state.completed)
        }
    }
}

@Composable
private fun statsViewModel(): StatsViewModel =
    appContainer().let { container ->
        viewModel { StatsViewModel(container) }
    }

@Composable
private fun StreakBand(
    state: StatsUi,
    onBack: () -> Unit,
) {
    WedgeBand(150.dp) {
        BackLink(stringResource(R.string.back), onBack, tint = BandInk)
        Text(
            pluralStringResource(R.plurals.streak_days, state.streak.current, state.streak.current),
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
        Bubble(96.dp, EF.Lime, Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 14.dp)) {
            Text(stringResource(R.string.in_a_row), style = bubbleText)
            Text(
                stringResource(R.string.best_streak, state.streak.best),
                style = bubbleText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Heatmap(state: StatsUi) {
    Column {
        Box(Modifier.height(16.dp)) {
            state.months.forEachIndexed { i, month ->
                if (month != null) {
                    Text(
                        month.getDisplayName(DateStyle.SHORT, Locale.ENGLISH),
                        style = T.label.copy(fontSize = 12.sp),
                        modifier = Modifier.offset(x = (CELL + GAP) * i),
                    )
                }
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(GAP)) {
            state.heat.forEachIndexed { w, week ->
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    week.forEachIndexed { d, level ->
                        HeatCell(
                            level,
                            today =
                                w == state.heat.lastIndex && level != null && week.getOrNull(d + 1) == null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatCell(
    level: Int?,
    today: Boolean,
) {
    val shape = RoundedCornerShape(3.dp)
    val color = level?.let { lerp(C.Line, EF.Teal, HEAT_SHARES[it]) } ?: Color.Transparent
    Box(
        Modifier
            .size(CELL)
            .background(color, shape)
            .then(if (today) Modifier.border(2.dp, C.Ink, shape) else Modifier),
    )
}

@Composable
private fun WeekLine(week: Week) {
    val bold = SpanStyle(color = C.Ink, fontWeight = FontWeight.SemiBold)
    val parts =
        listOf(
            pluralStringResource(R.plurals.week_days, week.days, week.days),
            pluralStringResource(R.plurals.week_answers, week.answers, week.answers),
            stringResource(R.string.week_minutes, week.minutes),
        )
    val prefix = stringResource(R.string.this_week)
    Text(
        buildAnnotatedString {
            append(prefix)
            parts.forEachIndexed { i, part ->
                append(if (i == 0) " " else " · ")
                withStyle(bold) { append(part) }
            }
        },
        style = T.label.copy(fontSize = 15.sp),
        modifier = Modifier.padding(top = 14.dp),
    )
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = T.label.copy(color = EF.Title, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(top = 32.dp),
    )
}

@Composable
private fun WeakSpots(
    spots: List<WeakSpot>,
    onSection: (sectionId: String) -> Unit,
) {
    Column {
        Heading(stringResource(R.string.weak_spots))
        spots.forEachIndexed { i, spot ->
            ListRow(onClick = { onSection(spot.section.id) }, divider = i != spots.lastIndex) {
                SectionTag(spot.section.kind)
                Column(Modifier.weight(1f)) {
                    Text(spot.section.title, style = T.rowEn)
                    Text(spot.chapter.id, style = T.rowSub, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    stringResource(R.string.percent, spot.accuracy.toPercent()),
                    style =
                        T.label.copy(
                            fontSize = 15.sp,
                            color =
                                if (spot.accuracy <
                                    Stats.BAD_BELOW
                                ) {
                                    C.Bad
                                } else {
                                    C.Muted
                                },
                        ),
                )
                Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
            }
        }
    }
}

@Composable
private fun Completed(completed: Int) {
    Column(Modifier.padding(bottom = 28.dp)) {
        Heading(stringResource(R.string.done))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressBar(completed.toFloat() / Stats.BOOK_CHAPTERS, Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.chapters_of, Stats.BOOK_CHAPTERS, completed, Stats.BOOK_CHAPTERS),
                style = T.label.copy(fontSize = 15.sp),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
