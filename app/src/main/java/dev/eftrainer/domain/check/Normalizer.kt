package dev.eftrainer.domain.check

object Normalizer {
    private val spaces = Regex("\\s+")
    private val finalPunctuation = Regex("[.?!]+$")

    fun normalize(input: String): String =
        input
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('–', '-')
            .replace('—', '-')
            .replace(spaces, " ")
            .trim()
            .replace(finalPunctuation, "")
            .trimEnd()
            .lowercase()
}
