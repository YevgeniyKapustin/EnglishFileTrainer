package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Task
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingQueueTest {
    private val passed = chapter("1A", section("a", 15))
    private val current = chapter("1B", section("g", 30), section("v", 5))
    private val chapters = listOf(passed, current)
    private val allMastered = (1..30).map { mastered("g-$it") } + (1..5).map { mastered("v-$it") }
    private val allSeen = (1..30).map { progress("g-$it", streak = 1) } + (1..5).map { progress("v-$it", streak = 1) }
    private val withPresets = listOf(passed, chapter("1B", section("g", 30).withPresets(3), section("v", 5)))

    private fun review(
        progress: Map<String, Progress>,
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

    @Test fun `a task session is that task's items, AI ones only with a key`() {
        val talk = Talk("g-talk", "Classmate.", "Ask.", "Hi!", listOf("asks"))
        val s = section("g", 3)
        val mixed =
            s.copy(
                tasks =
                    s.tasks + Task("g-b", Origin.SB, "Talk", listOf(Rewrite("g-9", "p", listOf("a")), talk)),
            )
        assertEquals(listOf("g-1", "g-2", "g-3"), TrainingQueue.task(mixed, "g-task").map { it.id })
        assertEquals(listOf("g-9"), TrainingQueue.task(mixed, "g-b").map { it.id })
        assertEquals(listOf("g-9", "g-talk"), TrainingQueue.task(mixed, "g-b", ai = true).map { it.id })
        assertEquals(emptyList<Any>(), TrainingQueue.task(mixed, "missing"))
    }

    @Test fun `due from passed chapters come first, most overdue first, max 10`() {
        val progress = ((1..15).map { mastered("a-$it", dueOn = TODAY.minusDays(it.toLong())) } + allSeen).byId()
        val plan = review(progress)
        assertEquals(10, plan.due.size)
        assertEquals("a-15", plan.due.first().id)
        assertEquals(TrainingQueue.SESSION_SIZE, plan.all.size)
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

    @Test fun `never answered book items stay out of review`() {
        val progress = listOf(progress("g-2"), progress("g-9")).byId()
        assertEquals(listOf("g-2", "g-9"), review(progress).current.map { it.id })
        assertTrue(review(emptyMap()).current.isEmpty())
    }

    @Test fun `book fallback comes in runs of neighbours in book order, weakest run first`() {
        val progress = ((1..9).map { progress("g-$it", streak = 2) } + progress("g-5", streak = 0)).byId()
        val ids = review(progress).current.map { it.id }
        assertEquals(listOf("g-4", "g-5", "g-6", "g-1", "g-2", "g-3", "g-7", "g-8", "g-9"), ids)
    }

    @Test fun `fallback runs from different tasks are interleaved`() {
        val s = section("g", 6)
        val twoTasks =
            s.copy(
                tasks =
                    s.tasks +
                        Task("g-b", Origin.SB, "Other", (1..6).map { Rewrite("g-b$it", "p", listOf("a")) }),
            )
        val chapters = listOf(chapter("1B", twoTasks))
        val progress =
            twoTasks.tasks
                .flatMap { it.exercises }
                .map { progress(it.id) }
                .byId()
        val ids = TrainingQueue.review(chapters, emptySet(), progress, TODAY).current.map { it.id }
        assertEquals(
            listOf("g-1", "g-2", "g-3", "g-b1", "g-b2", "g-b3", "g-4", "g-5", "g-6", "g-b4", "g-b5", "g-b6"),
            ids,
        )
    }

    @Test fun `answered today in current chapter is skipped`() {
        val progress = listOf(progress("g-1", lastAnsweredOn = TODAY), progress("g-2")).byId()
        assertEquals(listOf("g-2"), review(progress).current.map { it.id })
    }

    @Test fun `mastered mixed in at most one of five`() {
        val progress = ((1..9).map { mastered("g-$it") } + (10..30).map { progress("g-$it") }).byId()
        val cur = review(progress).current
        assertEquals(TrainingQueue.SESSION_SIZE, cur.size)
        assertEquals(4, cur.count { Mastery.isMastered(progress[it.id]) })
    }

    @Test fun `all mastered gives a short session`() {
        assertEquals(4, review(allMastered.byId()).current.size)
    }

    @Test fun `book finished leaves only due`() {
        val progress = listOf(mastered("a-1", dueOn = TODAY)).byId()
        val plan = review(progress, completed = setOf("1A", "1B"))
        assertEquals(listOf("a-1"), plan.all.map { it.id })
    }

    private fun reviewGen(
        progress: Map<String, Progress>,
        generated: Map<String, List<Exercise>>,
    ) = TrainingQueue.review(chapters, setOf("1A"), progress, TODAY, generated = generated)

    @Test fun `generated not shown while book of section is not seen`() {
        val progress = allSeen.filterNot { it.exerciseId == "g-30" }.byId()
        val ids = reviewGen(progress, mapOf("g" to generated("g", 5))).all.map { it.id }
        assertTrue(ids.none { "-gen-" in it })
        assertTrue("g-1" in ids)
    }

    @Test fun `fresh replace the book of a seen section`() {
        val progress = (allSeen + listOf(progress("g-gen-0001", streak = 1))).byId()
        val cur = reviewGen(progress, mapOf("g" to generated("g", 25))).current.map { it.id }
        assertEquals(TrainingQueue.SESSION_SIZE, cur.size)
        assertTrue(cur.all { "-gen-" in it })
        assertEquals("g-gen-0002", cur.first())
    }

    @Test fun `a section with fresh drops its book, one without keeps it`() {
        val cur = reviewGen(allSeen.byId(), mapOf("g" to generated("g", 2))).current.map { it.id }
        assertEquals(listOf("g-gen-0001", "g-gen-0002"), cur.take(2))
        assertTrue(cur.filter { it.startsWith("g-") }.all { "-gen-" in it })
        assertTrue("v-1" in cur)
    }

    @Test fun `fresh answered today leave the section to its book`() {
        val progress = (allSeen + generated("g", 2).map { progress(it.id, lastAnsweredOn = TODAY) }).byId()
        val cur = reviewGen(progress, mapOf("g" to generated("g", 2))).current.map { it.id }
        assertTrue(cur.none { "-gen-" in it })
        assertEquals(listOf("g-1", "g-2", "g-3"), cur.take(3))
    }

    @Test fun `skipped book items count as seen`() {
        val chapters = listOf(chapter("1B", section("g", 3).withPresets(2)))
        val progress = listOf(progress("g-1"), progress("g-2")).byId()
        val plain = TrainingQueue.review(chapters, emptySet(), progress, TODAY).current.map { it.id }
        val skipped =
            TrainingQueue.review(chapters, emptySet(), progress, TODAY, skipped = setOf("g-3")).current.map { it.id }
        assertEquals(listOf("g-1", "g-2"), plain)
        assertEquals(listOf("g-x1-01", "g-x1-02"), skipped)
    }

    @Test fun `weak generated come after unanswered ones, weakest first`() {
        val progress =
            (
                allSeen +
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

    @Test fun `mastered mix prefers fresh variants`() {
        val progress = (allMastered + generated("v", 5).map { mastered(it.id) }).byId()
        val cur = reviewGen(progress, mapOf("v" to generated("v", 5))).current.map { it.id }
        assertEquals(listOf("v-gen-0001", "v-gen-0002", "v-gen-0003", "v-gen-0004"), cur)
    }

    @Test fun `generated from completed chapters go by FSRS due`() {
        val progress =
            listOf(
                mastered("a-gen-0001", dueOn = TODAY.minusDays(2)),
                mastered("a-gen-0002", dueOn = TODAY.plusDays(5)),
            ).byId()
        assertEquals(listOf("a-gen-0001"), reviewGen(progress, mapOf("a" to generated("a", 3))).due.map { it.id })
    }

    @Test fun `presets not in first pass, not in mastery share, hidden until book seen`() {
        val g = withPresets[1].sections[0]
        assertEquals(30, TrainingQueue.firstPass(g, emptyMap()).size)
        assertEquals(1.0, Mastery.share(withPresets[1].exercises, allMastered.byId()), 1e-9)
        val progress = listOf(progress("g-1")).byId()
        val ids = TrainingQueue.review(withPresets, setOf("1A"), progress, TODAY).all.map { it.id }
        assertTrue(ids.none { "-x1-" in it })
    }

    @Test fun `presets come before AI-generated once book is seen`() {
        val cur =
            TrainingQueue
                .review(
                    withPresets,
                    setOf("1A"),
                    allSeen.byId(),
                    TODAY,
                    generated = mapOf("g" to generated("g", 3)),
                ).current
                .map { it.id }
                .filter { "-x1-" in it || "-gen-" in it }
        assertEquals(listOf("g-x1-01", "g-x1-02", "g-x1-03", "g-gen-0001", "g-gen-0002", "g-gen-0003"), cur)
    }
}
