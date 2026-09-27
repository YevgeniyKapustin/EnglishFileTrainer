package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Sort
import dev.eftrainer.data.content.Stress

object TapChecker {
    fun check(
        exercise: Stress,
        picks: List<String>,
    ): CheckResult {
        val chosen = picks.mapNotNull { it.toIntOrNull() }.toSet()
        val wrong = exercise.targets.filterTo(mutableSetOf()) { chosen.contains(it) != exercise.stressed.contains(it) }
        return CheckResult(correct = wrong.isEmpty(), expected = exercise.expected(), wrongGaps = wrong)
    }

    fun check(
        exercise: Sort,
        picks: List<String>,
    ): CheckResult {
        val chosen = picks.singleOrNull()?.toIntOrNull()
        return CheckResult(correct = chosen == exercise.answer, expected = exercise.expected())
    }
}

fun Stress.expected(): String = parts.mapIndexed { i, p -> if (i in stressed) p.uppercase() else p }.joinToString("")

private val MARKUP = Regex("""[\[\]|]""")

val Sort.plainWord: String get() = word.replace(MARKUP, "")

fun Sort.expected(): String = "$plainWord → ${columns[answer]}"
