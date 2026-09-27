package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.number
import dev.eftrainer.data.content.presets
import dev.eftrainer.data.generated.GeneratedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

fun generated(
    section: String,
    n: Int,
): List<Exercise> = (1..n).map { Rewrite("$section-gen-" + it.toString().padStart(4, '0'), "p$it", listOf("a$it")) }

class GenerationTest {
    private val cur = chapter("1B", section("g", 3), section("v", 2))
    private val withPresets = chapter("1B", section("g", 3).withPresets(4), section("v", 2))
    private val bookMastered = (1..3).map { mastered("g-$it") }

    @Test fun `nothing to generate until book of section is mastered`() {
        val progress = listOf(mastered("g-1"), mastered("g-2"), progress("g-3", streak = 2)).byId()
        assertEquals(emptyList<Any>(), GenerationPlan.refill(cur, progress, emptyMap()))
    }

    @Test fun `mastered section refilled up to stock when low`() {
        val progress = (1..3).map { mastered("g-$it") }.byId()
        val plan = GenerationPlan.refill(cur, progress, mapOf("g" to generated("g", 3)))
        assertEquals(listOf("g" to GenerationPlan.STOCK - 3), plan.map { it.first.id to it.second })
    }

    @Test fun `enough unanswered stock - no request`() {
        val progress = (1..3).map { mastered("g-$it") }.byId()
        assertEquals(
            emptyList<Any>(),
            GenerationPlan.refill(cur, progress, mapOf("g" to generated("g", GenerationPlan.LOW))),
        )
    }

    @Test fun `answered generated do not count as stock`() {
        val g = generated("g", 10)
        val progress = ((1..3).map { mastered("g-$it") } + g.take(5).map { progress(it.id) }).byId()
        assertEquals(
            listOf(GenerationPlan.STOCK - 5),
            GenerationPlan.refill(cur, progress, mapOf("g" to g)).map { it.second },
        )
    }

    @Test fun `generated ids resolve to a synthetic task in their section`() {
        val section =
            Chapters.all
                .first()
                .sections
                .first()
        val e = Blank("${section.id}-gen-0001", "Is {} ok?", listOf(listOf("it")))
        Chapters.registerGenerated(listOf(GeneratedItem(section.id, "Complete the questions.", e)))
        val loc = Chapters.locate(e.id)
        assertEquals(section.id, loc.section.id)
        assertEquals("${section.id}-gen", loc.task.id)
        assertEquals(Origin.GENERATED, loc.task.origin)
        assertEquals("Fresh practice: Complete the questions.", loc.task.instruction)
        assertEquals(listOf<Exercise>(e), loc.task.exercises)
        assertTrue(
            Chapters.all
                .first()
                .exercises
                .none { it.id == e.id },
        )
        assertEquals("Fresh practice", Chapters.locate("${section.id}-gen-9999").task.instruction)
    }

    @Test fun `no AI while presets are not all introduced`() {
        val progress = (bookMastered + (1..3).map { progress("g-x1-0$it") }).byId()
        assertEquals(emptyList<Any>(), GenerationPlan.refill(withPresets, progress, emptyMap()))
    }

    @Test fun `AI tops up after all presets were introduced`() {
        val progress = (bookMastered + (1..4).map { progress("g-x1-0$it") }).byId()
        assertEquals(
            listOf("g" to GenerationPlan.STOCK),
            GenerationPlan.refill(withPresets, progress, emptyMap()).map { it.first.id to it.second },
        )
    }

    @Test fun `preset ids resolve to their own task, not counted in chapter`() {
        val section =
            Chapters.all
                .first()
                .sections
                .first { it.presets.isNotEmpty() }
        val preset = section.presets.first()
        val loc = Chapters.locate(preset.id)
        assertEquals(section.id, loc.section.id)
        assertEquals(Origin.GENERATED, loc.task.origin)
        assertEquals("1", loc.task.number)
        assertTrue(
            Chapters.all
                .first()
                .exercises
                .none { it.id == preset.id },
        )
    }
}
