package dev.eftrainer.domain.check

object NumberWords {
    const val MAX_CARDINAL = 999
    private const val TEN = 10
    private const val TEEN_LIMIT = 20
    private const val HUNDRED = 100
    private val ORDINAL_RANGE = 1 until HUNDRED

    private val ones =
        listOf(
            "zero",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen",
        )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    private val irregularOrdinals =
        mapOf(
            "one" to "first",
            "two" to "second",
            "three" to "third",
            "five" to "fifth",
            "eight" to "eighth",
            "nine" to "ninth",
            "twelve" to "twelfth",
        )

    fun cardinal(n: Int): String =
        when {
            n < TEEN_LIMIT -> ones[n]
            n < HUNDRED -> tens[n / TEN] + if (n % TEN != 0) " " + ones[n % TEN] else ""
            else -> ones[n / HUNDRED] + " hundred" + if (n % HUNDRED != 0) " " + cardinal(n % HUNDRED) else ""
        }

    fun ordinal(n: Int): String? {
        if (n !in ORDINAL_RANGE) return null
        val words = cardinal(n).split(' ')
        return (words.dropLast(1) + ordinalWord(words.last())).joinToString(" ")
    }

    private fun ordinalWord(word: String): String =
        irregularOrdinals[word]
            ?: if (word.endsWith("y")) word.dropLast(1) + "ieth" else word + "th"
}
