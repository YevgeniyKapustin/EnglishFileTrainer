package dev.eftrainer.data.content

import dev.eftrainer.data.generated.GeneratedIds
import dev.eftrainer.data.generated.GeneratedItem
import java.util.concurrent.ConcurrentHashMap

/** Реестр глав в порядке книги. */
object Chapters {
    val all: List<Chapter> = listOf(File1A, File1B)

    /** Где лежит упражнение: нужно экрану, чтобы показать инструкцию задания. */
    data class Location(val chapter: Chapter, val section: Section, val task: Task)

    private val locations: Map<String, Location> by lazy {
        buildMap {
            for (c in all) for (s in c.sections) for (t in s.tasks) for (e in t.exercises) put(e.id, Location(c, s, t))
        }
    }
    private val sections: Map<String, Pair<Chapter, Section>> by lazy {
        all.flatMap { c -> c.sections.map { it.id to (c to it) } }.toMap()
    }

    // Свежие упражнения ИИ (Room, GeneratedRepository) — не в Section.tasks (заготовки "<раздел>-x<N>"
    // из JSON — в Section.tasks, их находит locations). Ни те, ни другие не входят в «освоено»
    // и «глава пройдена» (Section.exercises): прогресс главы считается только по книге.
    private val generated = ConcurrentHashMap<String, GeneratedItem>()

    fun registerGenerated(items: Collection<GeneratedItem>) {
        items.forEach { generated[it.exercise.id] = it }
    }

    fun locate(exerciseId: String): Location =
        locations[exerciseId] ?: locateGenerated(exerciseId) ?: throw NoSuchElementException("нет упражнения $exerciseId")

    /** Сгенерированное — в синтетическом задании "<раздел>-gen" своего раздела. */
    private fun locateGenerated(exerciseId: String): Location? {
        val (chapter, section) = GeneratedIds.sectionOf(exerciseId)?.let(sections::get) ?: return null
        val item = generated[exerciseId]
        val instruction = item?.instruction?.let { "$FRESH: $it" } ?: FRESH
        val task = Task("${section.id}-gen", Origin.GENERATED, instruction, listOfNotNull(item?.exercise))
        return Location(chapter, section, task)
    }

    private const val FRESH = "Fresh practice"

    fun section(sectionId: String): Section = sections.getValue(sectionId).second

    fun chapterOfSection(sectionId: String): Chapter = sections.getValue(sectionId).first
}
