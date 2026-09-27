package dev.eftrainer.ui.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.toPercent

private const val FILE_COLOR_COUNT = 3

private fun fileColor(file: Int): Color =
    when ((file - 1).mod(FILE_COLOR_COUNT)) {
        0 -> EF.Teal
        1 -> EF.File2
        else -> EF.File3
    }

@Composable
fun AllChaptersScreen(
    onBack: () -> Unit,
    onChapter: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: AllChaptersViewModel = allChaptersViewModel(),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        BackLink(stringResource(R.string.back), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(
                stringResource(R.string.all_chapters),
                style = T.title,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            rows.forEachIndexed { i, row ->
                val file = fileOf(row.chapter.id)
                if (rows.startsFile(i)) FileHeader(file, first = i == 0)
                ChapterItem(
                    row = row,
                    color = fileColor(file),
                    divider = i != rows.lastIndex,
                    onClick = {
                        vm.choose(row.chapter.id)
                        if (row.status == ChapterStatus.CURRENT) onBack() else onChapter(row.chapter.id)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun allChaptersViewModel(): AllChaptersViewModel =
    appContainer().let { container -> viewModel { AllChaptersViewModel(container) } }

@Composable
private fun FileHeader(
    file: Int,
    first: Boolean,
) {
    Text(
        stringResource(R.string.file_n, file).uppercase(),
        style =
            TextStyle(
                fontFamily = Golos,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.06.em,
                color = fileColor(file),
            ),
        modifier = Modifier.padding(top = if (first) 0.dp else 18.dp, bottom = 2.dp),
    )
}

@Composable
private fun ChapterItem(
    row: ChapterRow,
    color: Color,
    divider: Boolean,
    onClick: () -> Unit,
) {
    ListRow(onClick = onClick, divider = divider) {
        ChapterBadge(row.chapter.id, color)
        ChapterInfo(row, Modifier.weight(1f))
        StatusIcon(row.status)
    }
}

@Composable
private fun ChapterBadge(
    chapterId: String,
    color: Color,
) {
    Box(
        Modifier
            .size(
                44.dp,
                36.dp,
            ).clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            chapterId,
            style =
                TextStyle(
                    fontFamily = Golos,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
        )
    }
}

@Composable
private fun ChapterInfo(
    row: ChapterRow,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val titleColor =
            when (row.status) {
                ChapterStatus.CURRENT -> EF.Title
                ChapterStatus.COMPLETED -> C.Ink
                ChapterStatus.AHEAD -> C.Ink
            }
        Text(row.chapter.title, style = T.rowEn.copy(color = titleColor))
        when (row.status) {
            ChapterStatus.CURRENT -> {
                Text(
                    stringResource(R.string.mastered_share, row.mastered.toPercent()),
                    style = T.rowSub,
                )
            }

            ChapterStatus.COMPLETED -> {
                Text(
                    stringResource(R.string.completed),
                    style = T.rowSub.copy(color = C.Ok),
                )
            }

            ChapterStatus.AHEAD -> {}
        }
    }
}

@Composable
private fun StatusIcon(status: ChapterStatus) {
    when (status) {
        ChapterStatus.CURRENT, ChapterStatus.AHEAD -> {
            Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
        }

        ChapterStatus.COMPLETED -> {
            Icon(Icons.Check, null, Modifier.size(20.dp), tint = C.Ok)
        }
    }
}
