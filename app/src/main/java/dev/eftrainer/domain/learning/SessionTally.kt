package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Task
import dev.eftrainer.domain.check.WordBox

class SessionTally(
    exercises: List<Exercise>,
    resumed: TaskRun = TaskRun(),
) {
    private val queue = ArrayDeque(exercises.filterNot { it.id in resumed.passed || it.id in resumed.skipped })
    private val answered = (resumed.passed.keys + resumed.missed).toMutableSet()
    private val wrong = exercises.filterTo(linkedSetOf()) { it.id in resumed.missed }
    private val correctInputs = resumed.passed.toMutableMap()
    private val skipped = resumed.skipped.toMutableSet()

    var total = exercises.count { it.id !in resumed.skipped || it.id in answered }
        private set
    var done = exercises.count { it.id in resumed.passed }
        private set

    val current: Exercise get() = queue.first()
    val finished: Boolean get() = queue.isEmpty()
    val mistakes: List<Exercise> get() = wrong.toList()
    val firstTry: Int get() = total - wrong.size
    val seenIds: Set<String> get() = answered + queue.map { it.id }
    val run: TaskRun get() = TaskRun(correctInputs.toMap(), wrong.mapTo(mutableSetOf()) { it.id }, skipped.toSet())
    val order: List<String>
        get() {
            val waiting = queue.map { it.id }
            return (correctInputs.keys + skipped + wrong.map { it.id }).filterNot { it in waiting } + waiting
        }

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
        skipped += e.id
        if (e.id !in answered) total--
    }

    fun correctInputs(ids: Collection<String>): List<String> = ids.flatMap { correctInputs[it].orEmpty() }

    fun usedWords(task: Task): Set<String> =
        if (task.words.isEmpty()) emptySet() else WordBox.used(task, correctInputs(task.exercises.map { it.id }))

    companion object {
        fun resume(
            exercises: List<Exercise>,
            run: TaskRun,
        ): SessionTally = SessionTally(exercises, run).takeUnless { it.finished } ?: SessionTally(exercises)
    }
}
