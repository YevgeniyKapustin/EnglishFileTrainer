package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Task

object WordBox {
    private val Times = Regex("""\s*\(x(\d+)\)$""")

    fun used(
        task: Task,
        correctAnswers: Collection<String>,
    ): Set<String> {
        if (!eachWordOnce(task)) return emptySet()
        val given = correctAnswers.groupingBy(Normalizer::normalize).eachCount()
        return task.words.filter { (given[Normalizer.normalize(base(it))] ?: 0) >= times(it) }.toSet()
    }

    private fun base(word: String): String = word.replace(Times, "")

    private fun times(word: String): Int =
        Times
            .find(word)
            ?.groupValues
            ?.get(1)
            ?.toInt() ?: 1

    private fun eachWordOnce(task: Task): Boolean {
        val gaps = task.exercises.sumOf { (it as? Blank)?.gaps?.size ?: 1 }
        return task.words.isNotEmpty() && gaps <= task.words.sumOf(::times)
    }
}
