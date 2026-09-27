package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Task

object WordBox {
    fun used(
        task: Task,
        correctAnswers: Collection<String>,
    ): Set<String> {
        if (!eachWordOnce(task)) return emptySet()
        val given = correctAnswers.map(Normalizer::normalize).toSet()
        return task.words.filter { Normalizer.normalize(it) in given }.toSet()
    }

    private fun eachWordOnce(task: Task): Boolean {
        val gaps = task.exercises.sumOf { (it as? Blank)?.gaps?.size ?: 1 }
        return task.words.isNotEmpty() && gaps <= task.words.size
    }
}
