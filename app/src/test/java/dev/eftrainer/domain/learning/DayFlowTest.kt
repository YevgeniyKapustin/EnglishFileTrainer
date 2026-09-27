package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayFlowTest {
    private val first = chapter("1A", section("a", 3))
    private val second = chapter("1B", section("b", 25))
    private val third = chapter("1C", section("c", 2))
    private val chapters = listOf(first, second, third)
    private val yesterday: LocalDate = TODAY.minusDays(1)

    private fun step(
        progress: Map<String, Progress>,
        day: DayState = DayState(),
        completed: Set<String> = emptySet(),
    ): DayStep {
        val plan = TrainingQueue.review(chapters, completed, progress, TODAY)
        return DayFlow.step(chapters, completed, progress, plan, day, TODAY)
    }

    private fun answeredAll(
        vararg answered: Chapter,
        on: LocalDate = yesterday,
    ): Map<String, Progress> = answered.flatMap { it.exercises }.map { progress(it.id, lastAnsweredOn = on) }.byId()

    private fun firstAnsweredOn(
        progress: Map<String, Progress>,
        day: LocalDate,
    ): Map<String, LocalDate> = progress.keys.associateWith { day }

    @Test fun `review comes first while the current chapter has seen items`() {
        assertEquals(DayStep.Review, step(listOf(progress("a-1")).byId()))
    }

    @Test fun `due from passed chapters keep review going even after a finished review session`() {
        val progress = first.exercises.map { mastered(it.id, dueOn = TODAY) }.byId()
        val day = DayState(reviewedOn = TODAY)
        assertEquals(DayStep.Review, step(progress, day, completed = setOf("1A")))
    }

    @Test fun `review is over once a review session finished today and nothing is due`() {
        val progress = listOf(progress("a-1")).byId()
        assertEquals(DayStep.Continue(first), step(progress, DayState(reviewedOn = TODAY)))
    }

    @Test fun `nothing seen before means no review, straight to the chapter`() {
        assertEquals(DayStep.Continue(first), step(emptyMap()))
    }

    @Test fun `the chapter picked by hand is the actual one`() {
        val day = DayState(chosenChapter = "1B", reviewedOn = TODAY)
        assertEquals(DayStep.Continue(second), step(listOf(progress("a-1")).byId(), day))
    }

    @Test fun `a chapter gone through before today moves on to the next one`() {
        val progress = answeredAll(first)
        val day = DayState(reviewedOn = TODAY, firstAnswered = firstAnsweredOn(progress, yesterday))
        assertEquals(DayStep.Continue(second), step(progress, day))
    }

    @Test fun `a chapter gone through today is practised until tomorrow`() {
        val progress = answeredAll(first, on = TODAY)
        val day = DayState(reviewedOn = TODAY, firstAnswered = firstAnsweredOn(progress, TODAY))
        assertEquals(DayStep.Practice(first), step(progress, day))
    }

    @Test fun `continue is the unanswered rest of the chapter in book order, one session at a time`() {
        val progress = listOf(progress("b-1"), progress("b-3")).byId()
        val queue = DayFlow.continueQueue(second, progress).map { it.id }
        assertEquals(TrainingQueue.SESSION_SIZE, queue.size)
        assertEquals(listOf("b-2", "b-4", "b-5"), queue.take(3))
        assertEquals(23, DayFlow.left(second, progress))
    }

    @Test fun `skipped items leave continue and its count but stay in practice`() {
        val progress = listOf(progress("a-1")).byId()
        val skipped = setOf("a-2")
        assertEquals(listOf("a-3"), DayFlow.continueQueue(first, progress, skipped = skipped).map { it.id })
        assertEquals(1, DayFlow.left(first, progress, skipped))
        assertEquals(
            listOf("a-3"),
            TrainingQueue.firstPass(first.sections.single(), progress, skipped = skipped).map { it.id },
        )
        assertTrue("a-2" in DayFlow.practiceQueue(first, progress).map { it.id })
    }

    @Test fun `chapter with only skipped items left is not continued`() {
        val progress = answeredAll(first) - "a-3"
        val day = DayState(reviewedOn = TODAY, skipped = setOf("a-3"))
        assertEquals(DayStep.Continue(first), step(progress, DayState(reviewedOn = TODAY)))
        assertEquals(DayStep.Continue(second), step(progress, day))
    }

    @Test fun `practice is never empty after the chapter was answered today, weakest first`() {
        val progress =
            answeredAll(first, on = TODAY) +
                listOf(progress("a-2", lastAnsweredOn = TODAY).copy(correctCount = 0, wrongCount = 1)).byId()
        val queue = DayFlow.practiceQueue(first, progress).map { it.id }
        assertEquals(listOf("a-2", "a-1", "a-3"), queue)
        assertTrue(TrainingQueue.review(chapters, emptySet(), progress, TODAY).current.isEmpty())
    }

    @Test fun `whole book gone through with nothing to review rests`() {
        val progress = answeredAll(first, second, third).mapValues { (id, _) -> mastered(id) }
        val day = DayState(reviewedOn = TODAY, firstAnswered = firstAnsweredOn(progress, yesterday))
        assertEquals(DayStep.Rest, step(progress, day, completed = setOf("1A", "1B", "1C")))
    }
}
