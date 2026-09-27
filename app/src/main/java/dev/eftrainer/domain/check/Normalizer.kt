package dev.eftrainer.domain.check

/**
 * Приводит ответ к виду для сравнения (plans/architecture.md). Регистр и финальная
 * пунктуация в этих упражнениях не тренируются — штрафовать за них нечестно.
 */
object Normalizer {
    private val spaces = Regex("\\s+")
    private val finalPunctuation = Regex("[.?!]+$")

    fun normalize(input: String): String =
        input
            .replace('’', '\'')      // типографский апостроф ’ → '
            .replace('‘', '\'')      // и левая кавычка ‘, её тоже подставляют клавиатуры
            .replace(spaces, " ")
            .trim()
            .replace(finalPunctuation, "")
            .trimEnd()                    // "born ?" → "born"
            .lowercase()
}
