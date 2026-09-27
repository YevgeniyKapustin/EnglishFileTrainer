package dev.eftrainer.ui.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.Mastery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ChapterStatus { COMPLETED, CURRENT, AHEAD }

data class ChapterRow(
    val chapter: Chapter,
    val status: ChapterStatus,
    val mastered: Double,
)

class AllChaptersViewModel(
    container: AppContainer,
) : ViewModel() {
    val rows: StateFlow<List<ChapterRow>> =
        combine(container.chapters.completed, container.progress.progress, ::build).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(KEEP_ALIVE_MS),
            container.review.latest
                ?.let { build(it.completed, it.progress) }
                .orEmpty(),
        )

    private val day = container.day

    private fun build(
        completed: Set<String>,
        progress: Map<String, Progress>,
    ): List<ChapterRow> {
        val current = ChapterCompletion.current(Chapters.all, completed)
        return Chapters.all.map { ch ->
            val status =
                when {
                    ch.id in completed -> ChapterStatus.COMPLETED
                    ch == current -> ChapterStatus.CURRENT
                    else -> ChapterStatus.AHEAD
                }
            ChapterRow(ch, status, Mastery.share(ch.exercises, progress))
        }
    }

    fun choose(chapterId: String) = day.choose(chapterId)

    private companion object {
        const val KEEP_ALIVE_MS = 5_000L
    }
}

internal fun fileOf(chapterId: String): Int = Regex("""\d+""").find(chapterId)?.value?.toInt() ?: 1

internal fun List<ChapterRow>.startsFile(index: Int): Boolean =
    index == 0 || fileOf(this[index - 1].chapter.id) != fileOf(this[index].chapter.id)
