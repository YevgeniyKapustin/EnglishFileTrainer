package dev.eftrainer.data.content

import dev.eftrainer.data.generated.GeneratedIds
import dev.eftrainer.data.generated.GeneratedItem
import java.util.concurrent.ConcurrentHashMap

object Chapters {
    private const val FRESH = "Fresh practice"

    val all: List<Chapter> = listOf(File1A, File1B)

    data class Location(
        val chapter: Chapter,
        val section: Section,
        val task: Task,
    )

    private val locations: Map<String, Location> by lazy {
        buildMap {
            for (c in all) for (s in c.sections) for (t in s.tasks) for (e in t.exercises) put(e.id, Location(c, s, t))
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

    private fun locateGenerated(exerciseId: String): Location? {
        val (chapter, section) = GeneratedIds.sectionOf(exerciseId)?.let(sections::get) ?: return null
        val item = generated[exerciseId]
        val instruction = item?.instruction?.let { "$FRESH: $it" } ?: FRESH
        val task = Task("${section.id}-gen", Origin.GENERATED, instruction, listOfNotNull(item?.exercise))
        return Location(chapter, section, task)
    }

    fun section(sectionId: String): Section = sections.getValue(sectionId).second

    fun chapterOfSection(sectionId: String): Chapter = sections.getValue(sectionId).first
}
