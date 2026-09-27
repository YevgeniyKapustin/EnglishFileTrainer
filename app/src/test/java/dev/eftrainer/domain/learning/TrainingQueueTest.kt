package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.exercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingQueueTest {
    private val passed = chapter("1A", section("a", 15))
    private val current = chapter("1B", section("g", 30), section("v", 5))
    private val chapters = listOf(passed, current)
    private val allMastered = (1..30).map { mastered("g-$it") } + (1..5).map { mastered("v-$it") }
    private val withPresets = listOf(passed, chapter("1B", section("g", 30).withPresets(3), section("v", 5)))

    private fun review(
        progress: Map<String, dev.eftrainer.data.db.entities.Progress>,
        completed: Set<String> =
            setOf(
                "1A",
            ),
    ) = TrainingQueue.review(chapters, completed, progress, TODAY)

    @Test fun `fresh arrivals skip seen items and stop at the session limit`() {
        val fresh = generated("g", 8)
        val seen = setOf(fresh[0].id, fresh[1].id)
        assertEquals(fresh.drop(2).take(3), TrainingQueue.arrivals(fresh, seen, alreadyAdded = 2))
        assertEquals(emptyList<Any>(), TrainingQueue.arrivals(fresh, seen, TrainingQueue.FRESH_ARRIVALS))
    }

    @Test fun `first pass is unanswered in book order`() {
        val s = section("g", 5)
        val progress = listOf(progress("g-2"), progress("g-4")).byId()
        assertEquals(listOf("g-1", "g-3", "g-5"), TrainingQueue.firstPass(s, progress).map { it.id })
    }

    @Test fun `due from passed chapters come first, most overdue first, max 10`() {
        val progress = (1..15).map { mastered("a-$it", dueOn = TODAY.minusDays(it.toLong())) }.byId()
        val plan = review(progress)
        assertEquals(10, plan.due.size)
        assertEquals("a-15", plan.due.first().id)
        assertEquals(20, plan.all.size)
        assertEquals(plan.due, plan.all.take(10))
    }

    @Test fun `not yet due and answered today are skipped`() {
        val progress =
            listOf(
                mastered("a-1", dueOn = TODAY.plusDays(1)),
                mastered("a-2", dueOn = TODAY.minusDays(1), lastAnsweredOn = TODAY),
                mastered("a-3", dueOn = TODAY),
            ).byId()
        assertEquals(listOf("a-3"), review(progress).due.map { it.id })
    }

    @Test fun `current chapter only from open sections`() {
        val ids = review(emptyMap()).current.map { it.id }
        assertTrue(ids.all { it.startsWith("g-") })
    }

    @Test fun `weak answered go before new ones`() {
        val progress = listOf(progress("g-7", streak = 2), progress("g-3", streak = 0)).byId()
        val ids = review(progress).current.map { it.id }
        assertEquals(listOf("g-3", "g-7", "g-1", "g-2"), ids.take(4))
    }

    @Test fun `answered today in current chapter is skipped`() {
        val progress = listOf(progress("g-1", lastAnsweredOn = TODAY)).byId()
        assertTrue("g-1" !in review(progress).current.map { it.id })
    }

    @Test fun `mastered mixed in at most one of five`() {
        val progress = (1..10).map { mastered("g-$it") }.byId()
        val cur = review(progress).current
        assertEquals(20, cur.size)
        assertEquals(4, cur.count { Mastery.isMastered(progress[it.id]) })
    }

    @Test fun `all mastered gives a short session`() {
        val progress = ((1..30).map { mastered("g-$it") } + (1..5).map { mastered("v-$it") }).byId()
        assertEquals(4, review(progress).current.size)
    }

    @Test fun `book finished leaves only due`() {
        val progress = listOf(mastered("a-1", dueOn = TODAY)).byId()
        val plan = review(progress, completed = setOf("1A", "1B"))
        assertEquals(listOf("a-1"), plan.all.map { it.id })
    }

    private fun reviewGen(
        progress: Map<String, dev.eftrainer.data.db.entities.Progress>,
        generated: Map<String, List<dev.eftrainer.data.content.Exercise>>,
    ) = TrainingQueue.review(chapters, setOf("1A"), progress, TODAY, generated = generated)

    @Test fun `generated not shown while book of section is not mastered`() {
        val ids = reviewGen(emptyMap(), mapOf("g" to generated("g", 5))).all.map { it.id }
        assertTrue(ids.none { "-gen-" in it })
    }

    @Test fun `generated fill the session after book is mastered`() {
        val progress = (allMastered + listOf(progress("g-gen-0001", streak = 1))).byId()
        val cur = reviewGen(progress, mapOf("g" to generated("g", 25))).current
        assertEquals(TrainingQueue.SESSION_SIZE, cur.size)
        val fresh = cur.filter { "-gen-" in it.id }.map { it.id }
        assertEquals(16, fresh.size)
        assertEquals("g-gen-0002", fresh.first())
        assertTrue("g-gen-0001" !in fresh)
    }

    @Test fun `weak generated come after unanswered ones, weakest first`() {
        val progress =
            (
                allMastered +
                    listOf(
                        progress("g-gen-0001", streak = 2),
                        progress("g-gen-0002", streak = 0),
                    )
            ).byId()
        val fresh =
            reviewGen(
                progress,
                mapOf("g" to generated("g", 3)),
            ).current.filter { "-gen-" in it.id }.map { it.id }
        assertEquals(listOf("g-gen-0003", "g-gen-0002", "g-gen-0001"), fresh)
    }

    @Test fun `generated answered today skipped, unmastered book goes first`() {
        val progress =
            (
                allMastered +
                    listOf(
                        progress("g-1", streak = 1),
                        progress("v-gen-0001", lastAnsweredOn = TODAY),
                    )
            ).byId()
        val cur = reviewGen(progress, mapOf("v" to generated("v", 3))).current.map { it.id }
        assertTrue("v-gen-0001" !in cur)
        assertTrue(cur.indexOf("g-1") in 0 until cur.indexOf("v-gen-0002"))
    }

    @Test fun `generated from completed chapters go by FSRS due`() {
        val progress =
            listOf(
                mastered("a-gen-0001", dueOn = TODAY.minusDays(2)),
                mastered("a-gen-0002", dueOn = TODAY.plusDays(5)),
            ).byId()
        assertEquals(listOf("a-gen-0001"), reviewGen(progress, mapOf("a" to generated("a", 3))).due.map { it.id })
    }

    @Test fun `presets not in first pass, not in mastery share, hidden until book mastered`() {
        val g = withPresets[1].sections[0]
        assertEquals(30, TrainingQueue.firstPass(g, emptyMap()).size)
        assertTrue(ChapterCompletion.firstPassDone(g, allMastered.byId()))
        assertEquals(1.0, Mastery.share(withPresets[1].exercises, allMastered.byId()), 1e-9)
        val ids = TrainingQueue.review(withPresets, setOf("1A"), emptyMap(), TODAY).all.map { it.id }
        assertTrue(ids.none { "-x1-" in it })
    }

    @Test fun `presets come before AI-generated once book is mastered`() {
        val cur =
            TrainingQueue
                .review(
                    withPresets,
                    setOf("1A"),
                    allMastered.byId(),
                    TODAY,
                    generated = mapOf("g" to generated("g", 3)),
                ).current
                .map { it.id }
                .filter { "-x1-" in it || "-gen-" in it }
        assertEquals(listOf("g-x1-01", "g-x1-02", "g-x1-03", "g-gen-0001", "g-gen-0002", "g-gen-0003"), cur)
    }
}
