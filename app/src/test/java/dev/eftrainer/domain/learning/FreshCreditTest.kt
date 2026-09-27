package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.presets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreshCreditTest {
    private val g = section("g", 4).withPresets(2)
    private val chapters = listOf(chapter("1A", g, section("v", 2)))

    @Test fun `presets and generated items lead to their section, book items to none`() {
        assertEquals("g", FreshCredit.sectionOf(chapters, "g-x1-01")?.id)
        assertEquals("v", FreshCredit.sectionOf(chapters, "v-gen-0007")?.id)
        assertNull(FreshCredit.sectionOf(chapters, "g-1"))
        assertNull(FreshCredit.sectionOf(chapters, "iv-be"))
    }

    @Test fun `every real preset finds its section`() {
        Chapters.all.flatMap { it.sections }.forEach { s ->
            s.presets.forEach { assertEquals(s.id, FreshCredit.sectionOf(Chapters.all, it.id)?.id) }
        }
    }

    @Test fun `credit goes to the weakest answered book item not answered today`() {
        val progress =
            listOf(
                progress("g-1", streak = 2),
                progress("g-2", streak = 0, lastAnsweredOn = TODAY),
                progress("g-3", streak = 1),
                progress("g-x1-01", streak = 0),
            ).byId()
        assertEquals("g-3", FreshCredit.source(g, progress, TODAY)?.id)
    }

    @Test fun `ties go to the longest unanswered, then book order`() {
        val progress =
            listOf(
                progress("g-1", streak = 1),
                progress("g-2", streak = 1, lastAnsweredOn = TODAY.minusDays(3)),
                progress("g-3", streak = 1, lastAnsweredOn = TODAY.minusDays(3)),
            ).byId()
        assertEquals("g-2", FreshCredit.source(g, progress, TODAY)?.id)
    }

    @Test fun `all answered today still credits the weakest`() {
        val progress =
            listOf(progress("g-1", streak = 2, lastAnsweredOn = TODAY), progress("g-2", lastAnsweredOn = TODAY)).byId()
        assertEquals("g-2", FreshCredit.source(g, progress, TODAY)?.id)
    }

    @Test fun `never answered book items are never credited, so completion is untouched`() {
        assertNull(FreshCredit.source(g, emptyMap(), TODAY))
        val progress = listOf(progress("g-4", streak = 3)).byId()
        assertEquals("g-4", FreshCredit.source(g, progress, TODAY)?.id)
        assertEquals(listOf("g-4"), g.exercises.filter { it.id in progress }.map { it.id })
    }
}
