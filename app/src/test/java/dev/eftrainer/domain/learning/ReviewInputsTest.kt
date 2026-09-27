package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewInputsTest {
    private val talk = Talk("g-talk", "You are a classmate.", "Ask two questions.", "Hi!", listOf("asks questions"))
    private val g =
        section(
            "g",
            2,
        ).let { it.copy(tasks = it.tasks + Task("g-talk-task", Origin.SB, "Talk", listOf(talk))) }
    private val chapters = listOf(chapter("1A", g, section("v", 2)))

    private fun ids(
        ai: Boolean,
        generated: Map<String, List<dev.eftrainer.data.content.Exercise>> = emptyMap(),
        mastered: Boolean = false,
    ) = ReviewInputs(
        completed = emptySet(),
        progress = if (mastered) listOf(mastered("g-1"), mastered("g-2")).byId() else emptyMap(),
        ai = ai,
        generated = generated,
    ).plan(chapters, TODAY).all.map { it.id }

    @Test fun `talk counted only with AI`() {
        assertEquals(listOf("g-1", "g-2"), ids(ai = false))
        assertEquals(listOf("g-1", "g-2", "g-talk"), ids(ai = true))
    }

    @Test fun `fresh exercises counted as soon as they exist for a mastered section`() {
        val before = ids(ai = false, mastered = true)
        val after = ids(ai = false, generated = mapOf("g" to generated("g", 3)), mastered = true)
        assertEquals(3, after.size - before.size)
        assertEquals(listOf("g-gen-0001", "g-gen-0002", "g-gen-0003"), after.filter { "-gen-" in it })
    }

    @Test fun `presets counted once the book of the section is mastered`() {
        val chapters = listOf(chapter("1A", section("g", 2).withPresets(2)))

        fun ids(progress: List<dev.eftrainer.data.db.entities.Progress>) =
            ReviewInputs(
                emptySet(),
                progress.byId(),
                ai = false,
                generated = emptyMap(),
            ).plan(chapters, TODAY).all.map { it.id }
        assertEquals(listOf("g-1", "g-2"), ids(emptyList()))
        assertEquals(
            listOf("g-x1-01", "g-x1-02"),
            ids(listOf(mastered("g-1"), mastered("g-2"))).filter { "-x1-" in it },
        )
    }
}
