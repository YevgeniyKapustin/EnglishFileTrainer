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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

private fun ChapterRow.action(
    onCurrent: () -> Unit,
    onLocked: (chapterId: String) -> Unit,
    onCompleted: (chapterId: String) -> Unit,
): (() -> Unit)? =
    when (status) {
        ChapterStatus.CURRENT -> {
            onCurrent
        }

        ChapterStatus.LOCKED -> {
            { onLocked(chapter.id) }
        }

        ChapterStatus.COMPLETED -> {
            { onCompleted(chapter.id) }
        }
    }

@Composable
fun AllChaptersScreen(
    onBack: () -> Unit,
    onChapter: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: AllChaptersViewModel = allChaptersViewModel(),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    var ask by rememberSaveable { mutableStateOf<String?>(null) }

    ask?.let { id ->
        UnlockDialog(
            chapterId = id,
            before = rows.lockedBefore(id),
            onDismiss = { ask = null },
            onConfirm = {
                ask = null
                vm.unlock(id, onBack)
            },
        )
    }

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
                    onClick =
                        row.action(
                            onCurrent = {
                                vm.choose(row.chapter.id)
                                onBack()
                            },
                            onLocked = { ask = it },
                            onCompleted = {
                                vm.choose(it)
                                onChapter(it)
                            },
                        ),
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
private fun UnlockDialog(
    chapterId: String,
    before: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Paper,
        title = {
            Text(
                stringResource(R.string.unlock_title, chapterId),
                style = T.row.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                stringResource(R.string.unlock_text, before, chapterId),
                style = T.rowSub.copy(fontSize = 15.sp, lineHeight = 21.sp),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.unlock_confirm), color = C.Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = C.Muted) }
        },
    )
}

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
    onClick: (() -> Unit)?,
) {
    ListRow(onClick = onClick, divider = divider) {
        ChapterBadge(row.chapter.id, color, off = row.status == ChapterStatus.LOCKED)
        ChapterInfo(row, Modifier.weight(1f))
        StatusIcon(row.status)
    }
}

@Composable
private fun ChapterBadge(
    chapterId: String,
    color: Color,
    off: Boolean,
) {
    Box(
        Modifier
            .size(
                44.dp,
                36.dp,
            ).alpha(if (off) 0.35f else 1f)
            .clip(RoundedCornerShape(8.dp))
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
                ChapterStatus.LOCKED -> C.Off
            }
        Text(row.chapter.title, style = T.rowEn.copy(color = titleColor))
        when (row.status) {
            ChapterStatus.CURRENT -> {
                Text(
                    stringResource(R.string.next_opens_at, row.mastered.toPercent()),
                    style = T.rowSub,
                )
            }

            ChapterStatus.COMPLETED -> {
                Text(
                    stringResource(R.string.completed),
                    style = T.rowSub.copy(color = C.Ok),
                )
            }

            ChapterStatus.LOCKED -> {}
        }
    }
}

@Composable
private fun StatusIcon(status: ChapterStatus) {
    when (status) {
        ChapterStatus.CURRENT -> Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
        ChapterStatus.COMPLETED -> Icon(Icons.Check, null, Modifier.size(20.dp), tint = C.Ok)
        ChapterStatus.LOCKED -> Icon(Icons.Lock, null, Modifier.size(18.dp), tint = C.Off)
    }
}
