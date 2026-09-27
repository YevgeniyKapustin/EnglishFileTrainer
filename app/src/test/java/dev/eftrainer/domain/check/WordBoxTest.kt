package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class WordBoxTest {
    private fun task(
        words: List<String>,
        gaps: Int,
    ) = Task(
        "t",
        Origin.SB,
        "Complete",
        (1..gaps).map { Blank("t-$it", "x {}", listOf(listOf("a"))) },
        words = words,
    )

    @Test fun `correct answers strike their words`() {
        val t = task(listOf("clever", "shy", "kind"), gaps = 3)
        assertEquals(setOf("shy", "kind"), WordBox.used(t, listOf(" Shy", "kind", "bald")))
    }

    @Test fun `phrases match without final punctuation or curly apostrophes`() {
        val t = task(listOf("Do you have a good view?", "It's great to be here"), gaps = 2)
        assertEquals(
            setOf("Do you have a good view?", "It's great to be here"),
            WordBox.used(t, listOf("do you have a good view", "It’s great to be here")),
        )
    }

    @Test fun `words reused across items are never struck`() {
        val t = task(listOf("tree", "egg"), gaps = 5)
        assertEquals(emptySet<String>(), WordBox.used(t, listOf("tree")))
    }

    @Test fun `nothing is struck before any correct answer`() {
        assertEquals(emptySet<String>(), WordBox.used(task(listOf("a", "b"), gaps = 2), emptyList()))
    }
}
