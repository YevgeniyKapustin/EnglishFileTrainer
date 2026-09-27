package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress

/** Когда глава пройдена и что в ней открыто (plans/learning.md). */
object ChapterCompletion {
    const val THRESHOLD = 0.9

    fun isComplete(chapter: Chapter, progress: Map<String, Progress>): Boolean =
        Mastery.share(chapter.exercises, progress) >= THRESHOLD

    /** Текущая глава — первая непройденная; null, когда пройдена вся книга. */
    fun current(chapters: List<Chapter>, completed: Set<String>): Chapter? =
        chapters.firstOrNull { it.id !in completed }

    /** Первый проход раздела — на каждое упражнение ответил хотя бы раз. */
    fun firstPassDone(section: Section, progress: Map<String, Progress>): Boolean =
        section.exercises.all { it.id in progress }

    /** Открытые разделы главы: все сразу, как в книге (решено 27.09.2026); порядок книги — подсказка. */
    fun openSections(chapter: Chapter, progress: Map<String, Progress>): List<Section> = chapter.sections

    /**
     * Разделы, которые гоняет Review: начатые (есть хоть один ответ) и первый по книге.
     * Не начатые Review не трогает — новый раздел начинаешь сам, с правилами.
     */
    fun activeSections(chapter: Chapter, progress: Map<String, Progress>): List<Section> =
        chapter.sections.filterIndexed { i, s -> i == 0 || s.tasks.any { t -> t.exercises.any { it.id in progress } } }
}
