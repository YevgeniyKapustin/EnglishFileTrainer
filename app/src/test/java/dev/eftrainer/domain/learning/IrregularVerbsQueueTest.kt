package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.db.entities.Progress
import org.junit.Assert.assertEquals
import org.junit.Test

class IrregularVerbsQueueTest {
    private val verbs = section("iv", 14).exercises

    private fun ids(progress: List<Progress>) = TrainingQueue.verbs(verbs, progress.byId(), TODAY).map { it.id }

    private fun introducedToday(id: String) =
        progress(id, streak = 1, dueOn = TODAY.plusDays(2), lastAnsweredOn = TODAY)

    @Test fun `five new verbs a day in book order`() {
        assertEquals(listOf("iv-1", "iv-2", "iv-3", "iv-4", "iv-5"), ids(emptyList()))
    }

    @Test fun `nothing new once five were introduced today`() {
        assertEquals(emptyList<String>(), ids((1..5).map { introducedToday("iv-$it") }))
    }

    @Test fun `verbs introduced earlier today count toward the five`() {
        assertEquals(listOf("iv-3", "iv-4", "iv-5"), ids(listOf(introducedToday("iv-1"), introducedToday("iv-2"))))
    }

    @Test fun `reviewing an old verb today does not use up new ones`() {
        val reviewedToday = progress("iv-1", streak = 2, dueOn = TODAY.plusDays(5), lastAnsweredOn = TODAY)
        assertEquals(listOf("iv-2", "iv-3", "iv-4", "iv-5", "iv-6"), ids(listOf(reviewedToday)))
    }

    @Test fun `due verbs come first, most overdue first, then new ones`() {
        val progress =
            listOf(
                progress("iv-1", dueOn = TODAY.minusDays(1)),
                progress("iv-2", dueOn = TODAY.minusDays(3)),
                progress("iv-3", dueOn = TODAY.plusDays(5)),
            )
        assertEquals(listOf("iv-2", "iv-1", "iv-4", "iv-5", "iv-6", "iv-7", "iv-8"), ids(progress))
    }

    @Test fun `many due verbs are capped and leave no room for new ones`() {
        val progress = (1..12).map { progress("iv-$it", dueOn = TODAY.minusDays(1)) }
        assertEquals((1..TrainingQueue.VERB_LIMIT).map { "iv-$it" }, ids(progress))
    }

    @Test fun `verbs come on top of the chapter review`() {
        val chapters = listOf(chapter("1A", section("g", 30)))
        val plan = TrainingQueue.review(chapters, emptySet(), emptyMap(), TODAY, verbs = verbs)
        assertEquals(TrainingQueue.SESSION_SIZE, plan.current.size)
        assertEquals(TrainingQueue.NEW_VERBS_PER_DAY, plan.verbs.size)
        assertEquals(plan.verbs, plan.all.take(TrainingQueue.NEW_VERBS_PER_DAY))
    }

    @Test fun `verbs stay in review when the whole book is done`() {
        val chapters = listOf(chapter("1A", section("g", 3)))
        val plan = TrainingQueue.review(chapters, setOf("1A"), emptyMap(), TODAY, verbs = verbs)
        assertEquals(listOf("iv-1", "iv-2", "iv-3", "iv-4", "iv-5"), plan.all.map { it.id })
    }
}
