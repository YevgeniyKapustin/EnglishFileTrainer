package dev.eftrainer.ui.exercise.inputs

import dev.eftrainer.data.content.Blank

data class KeyWord(
    val key: String,
    val options: List<String>,
) {
    companion object {
        private val pattern = Regex("""^([^:→{}]+?)(?::\s*([^→{}]+?))?\s*→\s*\{\}$""")
        private val commas = Regex("""\s*,\s*""")
        private val spaces = Regex("""\s+""")

        fun parse(exercise: Blank): KeyWord? {
            val match = pattern.matchEntire(exercise.text.trim()) ?: return null
            val (key, rest) = match.destructured
            val options =
                when {
                    rest.isBlank() -> emptyList()
                    ',' in rest -> rest.split(commas)
                    else -> rest.split(spaces)
                }
            return KeyWord(key.trim(), options.filter { it.isNotBlank() })
        }
    }
}
