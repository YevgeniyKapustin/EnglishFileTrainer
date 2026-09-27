package dev.eftrainer.ui.section

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.ai.aiAvailable
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.Task
import dev.eftrainer.data.content.exercises
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.Mastery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TaskRow(
    val task: Task,
    val mastered: Int,
    val total: Int,
)

data class SectionUi(
    val section: Section,
    val chapterId: String,
    val tasks: List<TaskRow>,
    val started: Boolean,
    val firstPassDone: Boolean,
    val aiOn: Boolean,
)

class SectionViewModel(
    container: AppContainer,
    sectionId: String,
) : ViewModel() {
    private val section = Chapters.section(sectionId)
    private val chapterId = Chapters.chapterOfSection(sectionId).id

    val state: StateFlow<SectionUi?> =
        combine(
            container.progress.progress,
            aiAvailable(container.ai, container.connectivity),
        ) { progress, aiOn ->
            SectionUi(
                section = section,
                chapterId = chapterId,
                tasks =
                    section.tasks.filter { it.origin != Origin.GENERATED }.map { t ->
                        TaskRow(t, t.exercises.count { Mastery.isMastered(progress[it.id]) }, t.exercises.size)
                    },
                started = section.exercises.any { it.id in progress },
                firstPassDone = ChapterCompletion.firstPassDone(section, progress),
                aiOn = aiOn,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

internal fun List<TaskRow>.startsOriginGroup(index: Int): Boolean =
    index == 0 || this[index - 1].task.origin != this[index].task.origin

internal fun List<TaskRow>.endsOriginGroup(index: Int): Boolean =
    getOrNull(index + 1)?.task?.origin != this[index].task.origin
