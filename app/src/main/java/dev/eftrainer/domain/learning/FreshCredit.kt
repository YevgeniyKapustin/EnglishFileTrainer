package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.presets
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.data.generated.GeneratedIds
import java.time.LocalDate

object FreshCredit {
    fun sectionOf(
        chapters: List<Chapter>,
        exerciseId: String,
    ): Section? {
        val generatedIn = GeneratedIds.sectionOf(exerciseId)
        return chapters
            .flatMap { it.sections }
            .firstOrNull { s -> s.id == generatedIn || s.presets.any { it.id == exerciseId } }
    }

    fun source(
        section: Section,
        progress: Map<String, Progress>,
        today: LocalDate,
    ): Exercise? =
        section.exercises
            .filter { it.id in progress }
            .minWithOrNull(
                compareBy(
                    { progress.getValue(it.id).lastAnsweredOn == today },
                    { progress.getValue(it.id).streak },
                    { progress.getValue(it.id).lastAnsweredOn },
                ),
            )
}
