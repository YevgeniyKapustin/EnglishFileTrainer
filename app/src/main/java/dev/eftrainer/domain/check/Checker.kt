package dev.eftrainer.domain.check

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak

data class CheckResult(
    val correct: Boolean,
    val expected: String,               // что показать при ошибке: первый из допустимых вариантов
    val wrongGaps: Set<Int> = emptySet(), // для Blank — индексы неверных пропусков
)

/** Проверка ответа (plans/architecture.md). Чистая функция: без Android и без состояния. */
object Checker {

    fun check(exercise: Rewrite, input: String): CheckResult {
        val answer = Normalizer.normalize(input)
        return CheckResult(
            correct = exercise.answers.any { Normalizer.normalize(it) == answer },
            expected = exercise.answers.first(),
        )
    }

    fun check(exercise: Blank, inputs: List<String>): CheckResult {
        require(inputs.size == exercise.gaps.size) { "${exercise.id}: ждём ${exercise.gaps.size} ответов" }
        val wrong = exercise.gaps.indices.filterTo(mutableSetOf()) { i ->
            val answer = Normalizer.normalize(inputs[i])
            exercise.gaps[i].none { Normalizer.normalize(it) == answer }
        }
        return CheckResult(correct = wrong.isEmpty(), expected = filled(exercise), wrongGaps = wrong)
    }

    /**
     * Сказанное вслух: распознаватель сам ставит регистр, пунктуацию, цифры и сокращения,
     * поэтому сравниваем только слова (см. [words]).
     */
    fun check(exercise: Speak, input: String): CheckResult {
        val answer = words(input)
        val letters = letterGroup(exercise.text)
        return CheckResult(
            correct = exercise.answers.any { words(it) == answer } ||
                (letters != null && spokenLetters(input) == letters),
            expected = exercise.text,
        )
    }

    /**
     * Группа отдельных букв ("A K E", "Q, I, U") → "ake"; иначе null.
     * Для таких Speak проверка мягкая: см. [spokenLetters].
     */
    internal fun letterGroup(text: String): String? {
        val tokens = text.split(Regex("[\\s,\\-]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty() || tokens.any { it.length != 1 || it[0].lowercaseChar() !in 'a'..'z' }) return null
        return tokens.joinToString("") { it.lowercase() }
    }

    /**
     * Буквы, как их записал распознаватель: одиночную букву он часто пишет словом ("bee", "see",
     * "why", "double you"). Каждое слово → буква по [letterNames]; незнакомое слово — как есть
     * побуквенно ("AKE" → "ake"). Итог — буквы подряд: "hey kay e" → "ake".
     */
    internal fun spokenLetters(input: String): String {
        val tokens = Normalizer.normalize(input)
            .replace(Regex("[^a-z' ]"), " ")
            .split(' ')
            .map { it.trim('\'') }
            .filter { it.isNotEmpty() }
        return buildString {
            var i = 0
            while (i < tokens.size) {
                val t = tokens[i]
                if (t == "double" && tokens.getOrNull(i + 1) in setOf("you", "u", "yu", "ew", "ewe")) {
                    append('w'); i += 2; continue
                }
                append(letterNames[t] ?: t)
                i++
            }
        }
    }

    /** Как распознаватель пишет названия букв (en-US). "okay" — это O + K. */
    private val letterNames: Map<String, String> = mapOf(
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

    /**
     * Из гипотез распознавателя (лучшая первой) — первая, что засчитывается;
     * иначе самая вероятная. Так верная, но не первая гипотеза тоже идёт в зачёт.
     */
    fun bestHypothesis(exercise: Speak, hypotheses: List<String>): String =
        hypotheses.firstOrNull { check(exercise, it).correct } ?: hypotheses.firstOrNull().orEmpty()

    /**
     * Слова фразы в одном виде: "What's your name?" и "what is your name" совпадают.
     * - регистр и знаки препинания не важны, дефис = пробел;
     * - сокращения раскрываются: what's → what is, don't → do not, can't → can not;
     * - числа словами: 21 → twenty one, 7:30 → seven thirty, 3rd → third;
     * - буквы по одной склеиваются: "A-H-J-K", "a h j k", "AHJK" → ahjk (алфавит, spelling).
     */
    internal fun words(s: String): String {
        val tokens = Normalizer.normalize(s)
            .replace(Regex("[^a-z0-9' ]"), " ")
            .split(' ')
            .map { it.trim('\'') }
            .filter { it.isNotEmpty() }
            .flatMap { expand(it) }
        return glueLetters(tokens).joinToString(" ")
    }

    private val contractions = listOf(
        "n't" to " not", "'m" to " am", "'re" to " are", "'ll" to " will",
        "'ve" to " have", "'d" to " would", "'s" to " is",
    )
    private val special = mapOf(
        "can't" to "can not", "cannot" to "can not", "won't" to "will not",
        "ok" to "okay", "let's" to "let us",
    )

    private fun expand(token: String): List<String> {
        special[token]?.let { return it.split(' ') }
        token.toIntOrNull()?.let { n -> if (n in 0..999) return cardinal(n).split(' ') }
        Regex("(\\d+)(st|nd|rd|th)").matchEntire(token)?.let { m ->
            ordinal(m.groupValues[1].toInt())?.let { return it.split(' ') }
        }
        for ((suffix, full) in contractions) {
            if (token.endsWith(suffix) && token.length > suffix.length) {
                return (token.dropLast(suffix.length) + full).split(' ')
            }
        }
        return listOf(token)
    }

    /** Подряд идущие одиночные буквы (две и больше) — одно слово. */
    private fun glueLetters(tokens: List<String>): List<String> {
        val out = mutableListOf<String>()
        var run = StringBuilder()
        fun flush() {
            if (run.isNotEmpty()) out += run.toString()
            run = StringBuilder()
        }
        for (t in tokens) {
            if (t.length == 1 && t[0] in 'a'..'z') run.append(t) else { flush(); out += t }
        }
        flush()
        return out
    }

    private val ones = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    /** 0..999 словами без "and": 105 → "one hundred five" (с обеих сторон одинаково). */
    private fun cardinal(n: Int): String = when {
        n < 20 -> ones[n]
        n < 100 -> tens[n / 10] + if (n % 10 != 0) " " + ones[n % 10] else ""
        else -> ones[n / 100] + " hundred" + if (n % 100 != 0) " " + cardinal(n % 100) else ""
    }

    /** 1st..99th словами (даты, этажи); больше — null, оставляем как есть. */
    private fun ordinal(n: Int): String? {
        if (n !in 1..99) return null
        val irregular = mapOf(1 to "first", 2 to "second", 3 to "third", 5 to "fifth", 8 to "eighth", 9 to "ninth", 12 to "twelfth")
        irregular[n]?.let { return it }
        if (n >= 20 && n % 10 != 0) return tens[n / 10] + " " + ordinal(n % 10)
        val word = cardinal(n)
        return if (word.endsWith("y")) word.dropLast(1) + "ieth" else word + "th"
    }

    /** Текст с первыми вариантами в пропусках: "Where {} born?" → "Where were you born?". */
    private fun filled(exercise: Blank): String {
        val parts = exercise.text.split("{}")
        return buildString {
            parts.forEachIndexed { i, part ->
                append(part)
                if (i < exercise.gaps.size) append(exercise.gaps[i].first())
            }
        }
    }
}
