package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress

object ChapterCompletion {
    fun isComplete(
        chapter: Chapter,
        progress: Map<String, Progress>,
        skipped: Set<String> = emptySet(),
    ): Boolean = chapter.exercises.all { it.id in progress || it.id in skipped }

    fun current(
        chapters: List<Chapter>,
        completed: Set<String>,
    ): Chapter? = chapters.firstOrNull { it.id !in completed }

    fun firstPassDone(
        section: Section,
        progress: Map<String, Progress>,
        skipped: Set<String> = emptySet(),
    ): Boolean = section.exercises.all { it.id in progress || it.id in skipped }

    fun openSections(chapter: Chapter): List<Section> = chapter.sections

    fun activeSections(
        chapter: Chapter,
        progress: Map<String, Progress>,
    ): List<Section> =
        chapter.sections.filterIndexed { i, s -> i == 0 || s.tasks.any { t -> t.exercises.any { it.id in progress } } }
}
