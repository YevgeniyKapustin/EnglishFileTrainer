package dev.eftrainer.ui.stats

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
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
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.BandInk
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.core.ui.SectionTag
import dev.eftrainer.core.ui.WedgeBand
import dev.eftrainer.domain.learning.Stats
import dev.eftrainer.domain.learning.WeakSpot
import dev.eftrainer.domain.learning.Week
import dev.eftrainer.ui.appContainer
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as DateStyle

private val CELL = 14.dp
private val GAP = 4.dp
private val HEAT_SHARES = listOf(0f, .35f, .65f, 1f)
private const val HEAT_FROM = .12f
private const val HEAT_SPREAD = .4f
private const val CELL_MOTION = .22f
private const val ROW_STAGGER = .06f
private const val RAYS_SHARE = .35f
private const val TICK_QUIET = .2f
private const val TICK_RATE_FROM = .8f
private const val TICK_RATE_SPAN = .8f

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onSection: (sectionId: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: StatsViewModel = statsViewModel(),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val state = ui ?: return
    val intro = rememberStatsIntro()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StreakBand(state, intro, onBack)
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 22.dp)) {
            Heatmap(state, intro)
            state.week?.let { WeekLine(it, Modifier.rise(intro, .45f, .7f)) }
            if (state.weak.isNotEmpty()) WeakSpots(state.weak, intro, onSection)
            Completed(state.completed, intro)
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
    intro: StatsIntro,
    onBack: () -> Unit,
) {
    val days = (state.streak.current * intro.share(0f, .5f)).roundToInt()
    WedgeBand(150.dp, chapterId = state.chapterId, reveal = { intro.share(0f, RAYS_SHARE, LinearEasing) }) {
        BackLink(stringResource(R.string.back), onBack, tint = BandInk)
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 16.dp),
        ) {
            Text(
                pluralStringResource(R.plurals.streak_days, days, days),
                style =
                    TextStyle(
                        fontFamily = Golos,
                        fontSize = 44.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = BandInk,
                    ),
            )
            Text(
                stringResource(R.string.in_a_row),
                style = T.label.copy(fontSize = 17.sp, color = BandInk),
                modifier = Modifier.padding(top = 2.dp).popIn(intro, .2f, .55f),
            )
        }
    }
}

@Composable
private fun Heatmap(
    state: StatsUi,
    intro: StatsIntro,
) {
    val step = HEAT_SPREAD / state.heat.size.coerceAtLeast(1)
    HeatTicks(state.heat, intro, step)
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
                val start = HEAT_FROM + step * w
                Column(
                    Modifier.cellIn(intro, start, start + CELL_MOTION),
                    verticalArrangement = Arrangement.spacedBy(GAP),
                ) {
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
private fun HeatTicks(
    heat: List<List<Int?>>,
    intro: StatsIntro,
    step: Float,
) {
    val sounds = appContainer().sounds
    LaunchedEffect(intro) {
        if (!intro.fresh || heat.isEmpty()) return@LaunchedEffect
        val top = heat.maxOf { week -> week.sumOf { it ?: 0 } }.coerceAtLeast(1)
        var next = 0
        snapshotFlow { intro.clock.value }.collect { clock ->
            while (next < heat.size && clock >= HEAT_FROM + step * next) {
                val activity = heat[next].sumOf { it ?: 0 }.toFloat() / top
                val rate = TICK_RATE_FROM + TICK_RATE_SPAN * next / heat.lastIndex.coerceAtLeast(1)
                sounds.play(Cue.TICK, TICK_QUIET + (1f - TICK_QUIET) * activity, rate)
                next++
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
private fun WeekLine(
    week: Week,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.padding(top = 14.dp),
    )
}

@Composable
private fun Heading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = T.label.copy(color = EF.Title, fontWeight = FontWeight.SemiBold),
        modifier = modifier.padding(top = 32.dp),
    )
}

@Composable
private fun WeakSpots(
    spots: List<WeakSpot>,
    intro: StatsIntro,
    onSection: (sectionId: String) -> Unit,
) {
    Column {
        Heading(stringResource(R.string.weak_spots), Modifier.rise(intro, .5f, .75f))
        Text(
            stringResource(R.string.weak_spots_hint),
            style = T.label.copy(fontSize = 14.sp),
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp).rise(intro, .5f, .75f),
        )
        spots.forEachIndexed { i, spot ->
            val start = .55f + ROW_STAGGER * i
            ListRow(
                onClick = { onSection(spot.section.id) },
                divider = i != spots.lastIndex,
                modifier = Modifier.rise(intro, start, start + .25f),
            ) {
                SectionTag(spot.section.kind)
                Column(Modifier.weight(1f)) {
                    Text(spot.section.title, style = T.rowEn)
                    WeakScore(spot)
                }
                Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
            }
        }
    }
}

@Composable
private fun WeakScore(spot: WeakSpot) {
    val score = stringResource(R.string.right_of, spot.right, spot.total)
    val scoreColor = if (spot.accuracy < Stats.BAD_BELOW) C.Bad else C.Muted
    Text(
        buildAnnotatedString {
            append(spot.chapter.id)
            append(" · ")
            withStyle(SpanStyle(color = scoreColor)) { append(score) }
        },
        style = T.rowSub,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun Completed(
    completed: Int,
    intro: StatsIntro,
) {
    Column(Modifier.padding(bottom = 28.dp).rise(intro, .6f, .8f)) {
        Heading(stringResource(R.string.done))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressBar({ completed.toFloat() / Stats.BOOK_CHAPTERS * intro.share(.65f, 1f) }, Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.chapters_of, Stats.BOOK_CHAPTERS, completed, Stats.BOOK_CHAPTERS),
                style = T.label.copy(fontSize = 15.sp),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
