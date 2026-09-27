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
        progress =
            if (mastered) {
                listOf(mastered("g-1"), mastered("g-2")).byId()
            } else {
                listOf(progress("g-1"), progress("g-2"), progress("g-talk")).byId()
            },
        ai = ai,
        generated = generated,
    ).plan(chapters, TODAY).all.map { it.id }

    @Test fun `talk counted only with AI`() {
        assertEquals(listOf("g-1", "g-2"), ids(ai = false))
        assertEquals(listOf("g-1", "g-2", "g-talk"), ids(ai = true))
    }

    @Test fun `fresh exercises replace the book as soon as they exist for a seen section`() {
        assertEquals(listOf("g-1", "g-2"), ids(ai = false, mastered = true))
        assertEquals(
            listOf("g-gen-0001", "g-gen-0002", "g-gen-0003"),
            ids(ai = false, generated = mapOf("g" to generated("g", 3)), mastered = true),
        )
    }

    @Test fun `skipped items make a section seen`() {
        val chapters = listOf(chapter("1A", section("g", 2).withPresets(1)))
        val inputs = ReviewInputs(emptySet(), listOf(progress("g-1")).byId(), ai = false, generated = emptyMap())
        assertEquals(listOf("g-1"), inputs.plan(chapters, TODAY).all.map { it.id })
        assertEquals(
            listOf("g-x1-01"),
            inputs
                .copy(skipped = setOf("g-2"))
                .plan(chapters, TODAY)
                .all
                .map { it.id },
        )
    }

    @Test fun `presets counted once the book of the section is seen`() {
        val chapters = listOf(chapter("1A", section("g", 2).withPresets(2)))

        fun ids(progress: List<dev.eftrainer.data.db.entities.Progress>) =
            ReviewInputs(
                emptySet(),
                progress.byId(),
                ai = false,
                generated = emptyMap(),
            ).plan(chapters, TODAY).all.map { it.id }
        assertEquals(listOf("g-1"), ids(listOf(progress("g-1"))))
        assertEquals(listOf("g-x1-01", "g-x1-02"), ids(listOf(progress("g-1"), progress("g-2"))))
    }
}
