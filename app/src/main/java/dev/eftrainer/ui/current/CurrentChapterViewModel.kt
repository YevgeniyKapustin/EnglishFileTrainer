package dev.eftrainer.ui.current

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.exercises
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.Mastery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SectionRow(
    val section: Section,
    val open: Boolean,
    val mastered: Double,
    val opensAfter: String?,
)

data class CurrentUi(
    val chapter: Chapter?,
    val mastered: Double,
    val sections: List<SectionRow>,
    val reviewSections: List<String>,
    val reviewCurrent: Int,
    val reviewDue: Int,
    val past: Boolean,
)

class CurrentChapterViewModel(
    container: AppContainer,
    chapterId: String? = null,
) : ViewModel() {
    val state: StateFlow<CurrentUi?> =
        container.review.inputs
            .map { inputs ->
                val progress = inputs.progress
                val chapter =
                    chapterId?.let { id -> Chapters.all.first { it.id == id } }
                        ?: ChapterCompletion.current(Chapters.all, inputs.completed)
                val plan = container.review.plan(inputs)
                val open = chapter?.let { ChapterCompletion.openSections(it) }.orEmpty()
                val rows =
                    chapter?.sections.orEmpty().mapIndexed { i, s ->
                        SectionRow(
                            section = s,
                            open = s in open,
                            mastered = Mastery.share(s.exercises, progress),
                            opensAfter = if (s in open) null else chapter!!.sections[i - 1].title,
                        )
                    }
                CurrentUi(
                    chapter = chapter,
                    mastered = chapter?.let { Mastery.share(it.exercises, progress) } ?: 1.0,
                    sections = rows,
                    reviewSections = plan.current.map { Chapters.locate(it.id).section.title }.distinct(),
                    reviewCurrent = plan.current.size,
                    reviewDue = plan.due.size,
                    past = chapterId != null,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
