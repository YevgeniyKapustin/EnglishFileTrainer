package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak

data class CheckResult(
    val correct: Boolean,
    val expected: String,
    val wrongGaps: Set<Int> = emptySet(),
)

object Checker {
    private val letterNames: Map<String, String> =
        mapOf(
            "a" to listOf("ay", "eh", "hey", "ei"),
            "b" to listOf("be", "bee", "bea"),
            "c" to listOf("see", "sea", "cee", "si"),
            "d" to listOf("dee", "de", "di"),
            "e" to listOf("ee", "eee"),
            "f" to listOf("ef", "eff"),
            "g" to listOf("gee", "jee", "ji"),
            "h" to listOf("aitch", "haitch", "age", "eich"),
            "i" to listOf("eye", "aye", "ai"),
            "j" to listOf("jay", "jae"),
            "k" to listOf("kay", "kaye", "cay"),
            "l" to listOf("el", "ell", "elle"),
            "m" to listOf("em", "emm"),
            "n" to listOf("en", "enn"),
            "o" to listOf("oh", "owe", "ow"),
            "p" to listOf("pee", "pea", "pe"),
            "q" to listOf("queue", "cue", "kew", "que", "kyu"),
            "r" to listOf("are", "ar", "arr"),
            "s" to listOf("ess", "es"),
            "t" to listOf("tea", "tee", "ti"),
            "u" to listOf("you", "yu", "ewe", "yew"),
            "v" to listOf("vee", "ve", "vi"),
            "w" to listOf("doubleyou", "doubleu"),
            "x" to listOf("ex", "ecks", "eks"),
            "y" to listOf("why", "wye", "wy"),
            "z" to listOf("zed", "zee", "zet"),
            "ok" to listOf("okay", "ok"),
        ).flatMap { (letter, names) -> names.map { it to letter } }.toMap()

    private val contractions =
        listOf(
            "n't" to " not",
            "'m" to " am",
            "'re" to " are",
            "'ll" to " will",
            "'ve" to " have",
            "'d" to " would",
            "'s" to " is",
        )
    private val special =
        mapOf(
            "can't" to "can not",
            "cannot" to "can not",
            "won't" to "will not",
            "ok" to "okay",
            "let's" to "let us",
        )
    private val ORDINAL = Regex("""(\d+)(st|nd|rd|th)""")

    fun check(
        exercise: Rewrite,
        input: String,
    ): CheckResult {
        val answer = Normalizer.normalize(input)
        return CheckResult(
            correct = exercise.answers.any { Normalizer.normalize(it) == answer },
            expected = exercise.answers.first(),
        )
    }

    fun check(
        exercise: Blank,
        inputs: List<String>,
    ): CheckResult {
        require(inputs.size == exercise.gaps.size) { "${exercise.id}: ждём ${exercise.gaps.size} ответов" }
        val wrong =
            exercise.gaps.indices.filterTo(mutableSetOf()) { i ->
                val answer = Normalizer.normalize(inputs[i])
                exercise.gaps[i].none { Normalizer.normalize(it) == answer }
            }
        return CheckResult(correct = wrong.isEmpty(), expected = exercise.filled(), wrongGaps = wrong)
    }

    fun check(
        exercise: Order,
        labels: List<String>,
    ): CheckResult {
        require(labels.size == exercise.answer.size) { "${exercise.id}: ждём ${exercise.answer.size} картинок" }
        val wrong = exercise.answer.indices.filterTo(mutableSetOf()) { labels[it] != exercise.answer[it] }
        return CheckResult(correct = wrong.isEmpty(), expected = exercise.expected(), wrongGaps = wrong)
    }

    fun check(
        exercise: Speak,
        input: String,
    ): CheckResult {
        val answer = words(input)
        val letters = letterGroup(exercise.text)
        return CheckResult(
            correct =
                exercise.answers.any { words(it) == answer } ||
                    (letters != null && spokenLetters(input) == letters),
            expected = exercise.text,
        )
    }

    internal fun letterGroup(text: String): String? {
        val tokens = text.split(Regex("[\\s,\\-]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty() || tokens.any { it.length != 1 || it[0].lowercaseChar() !in 'a'..'z' }) return null
        return tokens.joinToString("") { it.lowercase() }
    }

    internal fun spokenLetters(input: String): String {
        val tokens =
            Normalizer
                .normalize(input)
                .replace(Regex("[^a-z' ]"), " ")
                .split(' ')
                .map { it.trim('\'') }
                .filter { it.isNotEmpty() }
        return buildString {
            var i = 0
            while (i < tokens.size) {
                val t = tokens[i]
                if (t == "double" && tokens.getOrNull(i + 1) in setOf("you", "u", "yu", "ew", "ewe")) {
                    append('w')
                    i += 2
                    continue
                }
                append(letterNames[t] ?: t)
                i++
            }
        }
    }

    fun bestHypothesis(
        exercise: Speak,
        hypotheses: List<String>,
    ): String = hypotheses.firstOrNull { check(exercise, it).correct } ?: hypotheses.firstOrNull().orEmpty()

    internal fun words(s: String): String {
        val tokens =
            Normalizer
                .normalize(s)
                .replace(Regex("[^a-z0-9' ]"), " ")
                .split(' ')
                .map { it.trim('\'') }
                .filter { it.isNotEmpty() }
                .flatMap { expand(it) }
        return glueLetters(tokens).joinToString(" ")
    }

    private fun expand(token: String): List<String> {
        val expanded =
            special[token]
                ?: token.toIntOrNull()?.takeIf { it in 0..NumberWords.MAX_CARDINAL }?.let(NumberWords::cardinal)
                ?: ORDINAL.matchEntire(token)?.let { NumberWords.ordinal(it.groupValues[1].toInt()) }
                ?: expandContraction(token)
                ?: token
        return expanded.split(' ')
    }

    private fun expandContraction(token: String): String? =
        contractions
            .firstOrNull { (suffix, _) -> token.endsWith(suffix) && token.length > suffix.length }
            ?.let { (suffix, full) -> token.dropLast(suffix.length) + full }

    private fun glueLetters(tokens: List<String>): List<String> {
        val out = mutableListOf<String>()
        var run = StringBuilder()

        fun flush() {
            if (run.isNotEmpty()) out += run.toString()
            run = StringBuilder()
        }
        for (t in tokens) {
            if (t.length == 1 && t[0] in 'a'..'z') {
                run.append(t)
            } else {
                flush()
                out += t
            }
        }
        flush()
        return out
    }
}

fun Blank.filled(): String {
    val parts = text.split("{}")
    return parts.mapIndexed { i, p -> p + (gaps.getOrNull(i)?.firstOrNull() ?: "") }.joinToString("")
}

fun Order.expected(): String = answer.joinToString(" ")

fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
