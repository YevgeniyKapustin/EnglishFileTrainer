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

    fun openSections(chapter: Chapter): List<Section> = chapter.sections
}
