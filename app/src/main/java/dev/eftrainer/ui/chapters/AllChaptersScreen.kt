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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.AppContainer
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.exercises
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.Mastery
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.toPercent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChapterStatus { COMPLETED, CURRENT, LOCKED }

data class ChapterRow(val chapter: Chapter, val status: ChapterStatus, val mastered: Double)

class AllChaptersViewModel(container: AppContainer) : ViewModel() {
    val rows: StateFlow<List<ChapterRow>> =
        combine(container.chapters.completed, container.progress.progress) { completed, progress ->
            val current = ChapterCompletion.current(Chapters.all, completed)
            Chapters.all.map { ch ->
                val status = when {
                    ch.id in completed -> ChapterStatus.COMPLETED
                    ch == current -> ChapterStatus.CURRENT
                    else -> ChapterStatus.LOCKED
                }
                ChapterRow(ch, status, Mastery.share(ch.exercises, progress))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val chapters = container.chapters
    private val clock = container.clock

    /**
     * Открыть главу досрочно: все главы до неё считаются пройденными — их отвеченное уходит
     * в повторение по датам FSRS, неотвеченное в Review больше не придёт.
     */
    fun unlock(chapterId: String, then: () -> Unit) {
        viewModelScope.launch {
            val completed = chapters.getCompleted()
            val today = clock.today()
            Chapters.all.takeWhile { it.id != chapterId }
                .filter { it.id !in completed }
                .forEach { chapters.markCompleted(it.id, today) }
            then()
        }
    }
}


/** Номер File по id главы: «1A» → 1, «PE1» → 1. */
private fun fileOf(chapterId: String): Int = Regex("""\d+""").find(chapterId)?.value?.toInt() ?: 1

/** Цвет File, как в учебнике: бирюзовый, оранжевый, зелёный — и по кругу. */
private fun fileColor(file: Int): Color = when ((file - 1).mod(3)) {
    0 -> EF.Teal
    1 -> EF.File2
    else -> EF.File3
}

@Composable
fun AllChaptersScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm = viewModel { AllChaptersViewModel(container) }
    val rows by vm.rows.collectAsStateWithLifecycle()
    var ask by rememberSaveable { mutableStateOf<String?>(null) }   // глава, которую открыть досрочно

    ask?.let { id ->
        val before = rows.takeWhile { it.chapter.id != id }.filter { it.status != ChapterStatus.COMPLETED }
            .joinToString { it.chapter.id }
        AlertDialog(
            onDismissRequest = { ask = null },
            containerColor = C.Paper,
            title = { Text(stringResource(R.string.unlock_title, id), style = T.row.copy(fontWeight = FontWeight.Bold)) },
            text = { Text(stringResource(R.string.unlock_text, before, id), style = T.rowSub.copy(fontSize = 15.sp, lineHeight = 21.sp)) },
            confirmButton = {
                TextButton(onClick = { ask = null; vm.unlock(id, onBack) }) {
                    Text(stringResource(R.string.unlock_confirm), color = C.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ask = null }) { Text(stringResource(R.string.cancel), color = C.Muted) }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        BackLink(stringResource(R.string.back), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.all_chapters), style = T.title, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
            rows.forEachIndexed { i, row ->
                val file = fileOf(row.chapter.id)
                val color = fileColor(file)
                if (i == 0 || fileOf(rows[i - 1].chapter.id) != file) {
                    Text(
                        stringResource(R.string.file_n, file).uppercase(),
                        style = TextStyle(fontFamily = Golos, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = color),
                        modifier = Modifier.padding(top = if (i == 0) 0.dp else 18.dp, bottom = 2.dp),
                    )
                }
                val off = row.status == ChapterStatus.LOCKED
                ListRow(
                    onClick = when (row.status) {
                        ChapterStatus.CURRENT -> onBack
                        ChapterStatus.LOCKED -> { { ask = row.chapter.id } }
                        ChapterStatus.COMPLETED -> null
                    },
                    divider = i != rows.lastIndex,
                ) {
                    Box(
                        Modifier.size(44.dp, 36.dp).alpha(if (off) 0.35f else 1f).clip(RoundedCornerShape(8.dp)).background(color),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(row.chapter.id, style = TextStyle(fontFamily = Golos, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    }
                    Column(Modifier.weight(1f)) {
                        val titleColor = when (row.status) {
                            ChapterStatus.CURRENT -> EF.Title
                            ChapterStatus.COMPLETED -> C.Ink
                            ChapterStatus.LOCKED -> C.Off
                        }
                        Text(row.chapter.title, style = T.rowEn.copy(color = titleColor))
                        when (row.status) {
                            ChapterStatus.CURRENT -> Text(stringResource(R.string.next_opens_at, row.mastered.toPercent()), style = T.rowSub)
                            ChapterStatus.COMPLETED -> Text(stringResource(R.string.completed), style = T.rowSub.copy(color = C.Ok))
                            ChapterStatus.LOCKED -> Unit
                        }
                    }
                    when (row.status) {
                        ChapterStatus.CURRENT -> Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
                        ChapterStatus.COMPLETED -> Icon(Icons.Check, null, Modifier.size(20.dp), tint = C.Ok)
                        ChapterStatus.LOCKED -> Icon(Icons.Lock, null, Modifier.size(18.dp), tint = C.Off)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
