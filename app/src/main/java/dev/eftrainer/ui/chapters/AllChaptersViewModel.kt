package dev.eftrainer.ui.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.exercises
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.Mastery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChapterStatus { COMPLETED, CURRENT, LOCKED }

data class ChapterRow(
    val chapter: Chapter,
    val status: ChapterStatus,
    val mastered: Double,
)

class AllChaptersViewModel(
    container: AppContainer,
) : ViewModel() {
    val rows: StateFlow<List<ChapterRow>> =
        combine(container.chapters.completed, container.progress.progress) { completed, progress ->
            val current = ChapterCompletion.current(Chapters.all, completed)
            Chapters.all.map { ch ->
                val status =
                    when {
                        ch.id in completed -> ChapterStatus.COMPLETED
                        ch == current -> ChapterStatus.CURRENT
                        else -> ChapterStatus.LOCKED
                    }
                ChapterRow(ch, status, Mastery.share(ch.exercises, progress))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val chapters = container.chapters
    private val clock = container.clock

    fun unlock(
        chapterId: String,
        then: () -> Unit,
    ) {
        viewModelScope.launch {
            val completed = chapters.getCompleted()
            val today = clock.today()
            Chapters.all
                .takeWhile { it.id != chapterId }
                .filter { it.id !in completed }
                .forEach { chapters.markCompleted(it.id, today) }
            then()
        }
    }
}

internal fun fileOf(chapterId: String): Int = Regex("""\d+""").find(chapterId)?.value?.toInt() ?: 1

internal fun List<ChapterRow>.startsFile(index: Int): Boolean =
    index == 0 || fileOf(this[index - 1].chapter.id) != fileOf(this[index].chapter.id)

internal fun List<ChapterRow>.lockedBefore(chapterId: String): String =
    takeWhile { it.chapter.id != chapterId }
        .filter { it.status != ChapterStatus.COMPLETED }
        .joinToString { it.chapter.id }
