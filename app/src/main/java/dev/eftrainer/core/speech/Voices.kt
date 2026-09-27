package dev.eftrainer.core.speech

import java.util.Locale

data class VoiceInfo(
    val name: String,
    val locale: Locale,
)

data class VoiceChoice(
    val name: String,
    val accent: String,
    val number: Int,
)

data class VoiceGroup(
    val accent: String,
    val voices: List<VoiceChoice>,
)

fun accentOf(locale: Locale): String =
    when (locale.country.uppercase()) {
        "US" -> "American"
        "GB" -> "British"
        "AU" -> "Australian"
        "IN" -> "Indian"
        "" -> "English"
        else -> locale.getDisplayCountry(Locale.ENGLISH).ifBlank { locale.country }
    }

fun groupVoices(voices: List<VoiceInfo>): List<VoiceGroup> {
    val order = listOf("American", "British", "Australian", "Indian")
    return voices
        .distinctBy { it.name }
        .groupBy { accentOf(it.locale) }
        .toList()
        .sortedWith(compareBy({ order.indexOf(it.first).let { i -> if (i < 0) order.size else i } }, { it.first }))
        .map { (accent, list) ->
            VoiceGroup(accent, list.sortedBy { it.name }.mapIndexed { i, v -> VoiceChoice(v.name, accent, i + 1) })
        }
}
