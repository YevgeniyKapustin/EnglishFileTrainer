package dev.eftrainer.domain.ai

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.SectionKind

object RuleScope {
    fun unitOf(
        chapters: List<Chapter>,
        chapter: Chapter,
    ): List<Chapter> {
        val unit = unitNumber(chapter)
        return chapters.take(chapters.indexOf(chapter) + 1).filter { unitNumber(it) == unit }
    }

    fun forExercise(
        chapters: List<Chapter>,
        chapter: Chapter,
        section: Section,
    ): List<Section> = if (section.kind == SectionKind.REVIEW) unitLessons(chapters, chapter) else listOf(section)

    fun forChapter(
        chapters: List<Chapter>,
        chapter: Chapter,
        section: Section,
    ): List<Section> = if (section.kind == SectionKind.REVIEW) unitLessons(chapters, chapter) else chapter.sections

    private fun unitLessons(
        chapters: List<Chapter>,
        chapter: Chapter,
    ): List<Section> = unitOf(chapters, chapter).flatMap { it.sections }.filter { it.kind != SectionKind.REVIEW }

    private fun unitNumber(chapter: Chapter): String = chapter.id.takeWhile { it.isDigit() }
}
