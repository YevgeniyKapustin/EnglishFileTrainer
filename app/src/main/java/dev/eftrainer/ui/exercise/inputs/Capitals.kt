package dev.eftrainer.ui.exercise.inputs

object Capitals {
    private const val GAP = "{}"
    private val sentenceEnds = setOf('.', '?', '!', ':', '\n')
    private val afterSentence = Regex("""([.?!]\s+)([a-z])""")
    private val pronounI = Regex("""(?<![\p{L}'’])i(?=[\s'’,.!?])""")

    fun fix(
        text: String,
        sentenceStart: Boolean,
    ): String {
        val sentences = afterSentence.replace(text) { it.groupValues[1] + it.groupValues[2].uppercase() }
        val pronouns = pronounI.replace(sentences, "I")
        if (!sentenceStart) return pronouns
        val first = pronouns.indexOfFirst { !it.isWhitespace() }
        if (first < 0 || pronouns[first] !in 'a'..'z') return pronouns
        return pronouns.substring(0, first) + pronouns[first].uppercaseChar() + pronouns.substring(first + 1)
    }

    fun gapStarts(text: String): List<Boolean> =
        text.split(GAP).dropLast(1).map { before ->
            val trimmed = before.trimEnd(' ', '\t')
            trimmed.isEmpty() || trimmed.last() in sentenceEnds
        }
}
