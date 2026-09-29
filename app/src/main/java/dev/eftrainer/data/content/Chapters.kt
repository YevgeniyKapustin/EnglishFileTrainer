package dev.eftrainer.data.content

import dev.eftrainer.data.generated.GeneratedIds
import dev.eftrainer.data.generated.GeneratedItem
import java.util.concurrent.ConcurrentHashMap

object Chapters {
    private const val FRESH = "Fresh practice"

    val all: List<Chapter> =
        listOf(
            File1A,
            File1B,
            File1C,
            File2A,
            File2B,
            File2C,
            File3A,
            File3B,
            File3C,
            File4A,
            File4B,
            File4C,
            File5A,
            File5B,
            File5C,
            File6A,
            File6B,
            File6C,
            File7A,
            File7B,
            File7C,
            File8A,
            File8B,
            File8C,
            File9A,
            File9B,
            File9C,
            File10A,
            File10B,
            File10C,
            File11A,
            File11B,
            File11C,
            File12A,
            File12B,
            File12C,
        )

    data class Location(
        val chapter: Chapter,
        val section: Section,
        val task: Task,
    )

    private val locations: Map<String, Location> by lazy {
        buildMap {
            for (c in all + IrregularVerbs.chapter) {
                for (s in c.sections) {
                    for (t in s.tasks) {
                        for (e in t.exercises) {
                            put(
                                e.id,
                                Location(c, s, t.marked()),
                            )
                        }
                    }
                }
            }
        }
    }
    private val sections: Map<String, Pair<Chapter, Section>> by lazy {
        all.flatMap { c -> c.sections.map { it.id to (c to it) } }.toMap()
    }

    private val generated = ConcurrentHashMap<String, GeneratedItem>()

    fun registerGenerated(items: Collection<GeneratedItem>) {
        items.forEach { generated[it.exercise.id] = it }
    }

    fun locate(exerciseId: String): Location =
        locations[exerciseId] ?: locateGenerated(
            exerciseId,
        ) ?: throw NoSuchElementException("нет упражнения $exerciseId")

    fun exerciseOrNull(exerciseId: String): Exercise? =
        (locations[exerciseId] ?: locateGenerated(exerciseId))?.task?.exercises?.find { it.id == exerciseId }

    private fun locateGenerated(exerciseId: String): Location? {
        val (chapter, section) = GeneratedIds.sectionOf(exerciseId)?.let(sections::get) ?: return null
        val item = generated[exerciseId]
        val task = Task("${section.id}-gen", Origin.GENERATED, fresh(item?.instruction), listOfNotNull(item?.exercise))
        return Location(chapter, section, task)
    }

    private fun Task.marked(): Task = if (origin == Origin.GENERATED) copy(instruction = fresh(instruction)) else this

    private fun fresh(instruction: String?): String = instruction?.let { "$FRESH: $it" } ?: FRESH

    fun section(sectionId: String): Section = sections.getValue(sectionId).second

    fun chapterOfSection(sectionId: String): Chapter = sections.getValue(sectionId).first
}
