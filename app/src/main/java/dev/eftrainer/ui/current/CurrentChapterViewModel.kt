package dev.eftrainer.ui.current

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.exercises
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.DayFlow
import dev.eftrainer.domain.learning.DayInputs
import dev.eftrainer.domain.learning.DayStep
import dev.eftrainer.domain.learning.Mastery
import dev.eftrainer.domain.learning.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class SectionRow(
    val section: Section,
    val mastered: Double,
)

data class CurrentUi(
    val chapter: Chapter?,
    val mastered: Double,
    val sections: List<SectionRow>,
    val reviewSections: List<String>,
    val reviewCurrent: Int,
    val reviewDue: Int,
    val past: Boolean,
    val streak: Int,
    val step: DayStep,
    val left: Int,
)

class CurrentChapterViewModel(
    private val container: AppContainer,
    private val chapterId: String? = null,
) : ViewModel() {
    val state: StateFlow<CurrentUi?> =
        combine(container.day.inputs, container.answers.days, ::build)
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE_MS), cached())

    private fun cached(): CurrentUi? {
        val inputs = container.day.latest ?: return null
        val days = container.answers.latestDays ?: return null
        return build(inputs, days)
    }

    private fun build(
        day: DayInputs,
        days: Set<LocalDate>,
    ): CurrentUi {
        val inputs = day.review
        val progress = inputs.progress
        val chapter =
            chapterId?.let { id -> Chapters.all.first { it.id == id } }
                ?: ChapterCompletion.current(Chapters.all, inputs.completed)
        val dayPlan = container.day.plan(day)
        val plan = dayPlan.review
        val step = dayPlan.step
        val rows = chapter?.sections.orEmpty().map { SectionRow(it, Mastery.share(it.exercises, progress)) }
        return CurrentUi(
            chapter = chapter,
            mastered = chapter?.let { Mastery.share(it.exercises, progress) } ?: 1.0,
            sections = rows,
            reviewSections = (plan.current + plan.verbs).map { Chapters.locate(it.id).section.title }.distinct(),
            reviewCurrent = plan.current.size + plan.verbs.size,
            reviewDue = plan.due.size,
            past = chapterId != null,
            streak = Stats.streak(days, container.clock.today()).current,
            step = step,
            left = (step as? DayStep.Continue)?.let { DayFlow.left(it.chapter, progress, day.day.skipped) } ?: 0,
        )
    }

    private companion object {
        const val KEEP_ALIVE_MS = 5_000L
    }
}
