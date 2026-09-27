package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Task
import dev.eftrainer.domain.check.WordBox

class SessionTally(
    exercises: List<Exercise>,
) {
    private val queue = ArrayDeque(exercises)
    private val answered = mutableSetOf<String>()
    private val wrong = linkedSetOf<Exercise>()
    private val correctInputs = mutableMapOf<String, List<String>>()

    var total = exercises.size
        private set
    var done = 0
        private set

    val current: Exercise get() = queue.first()
    val finished: Boolean get() = queue.isEmpty()
    val mistakes: List<Exercise> get() = wrong.toList()
    val firstTry: Int get() = total - wrong.size
    val seenIds: Set<String> get() = answered + queue.map { it.id }

    fun add(more: List<Exercise>) {
        queue.addAll(more)
        total += more.size
    }

    fun answer(
        exercise: Exercise,
        correct: Boolean,
        inputs: List<String>,
    ): Boolean {
        if (correct) {
            done++
            correctInputs[exercise.id] = inputs
        } else {
            wrong += exercise
        }
        return answered.add(exercise.id)
    }

    fun next(correct: Boolean) {
        val e = queue.removeFirst()
        if (!correct) queue.addLast(e)
    }

    fun skip() {
        val e = queue.removeFirst()
        if (e.id !in answered) total--
    }

    fun correctInputs(ids: Collection<String>): List<String> = ids.flatMap { correctInputs[it].orEmpty() }

    fun usedWords(task: Task): Set<String> =
        if (task.words.isEmpty()) emptySet() else WordBox.used(task, correctInputs(task.exercises.map { it.id }))
}
