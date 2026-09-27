package dev.eftrainer.core.speech

import java.util.Locale

/** Английский голос синтезатора: [name] — системное имя (en-us-x-iol-local), его и храним в настройках. */
data class VoiceInfo(val name: String, val locale: Locale)

/** Голос в выборе: акцент и номер внутри акцента («American · 2»). */
data class VoiceChoice(val name: String, val accent: String, val number: Int)

/** Группа голосов одного акцента. */
data class VoiceGroup(val accent: String, val voices: List<VoiceChoice>)

/** Понятное название акцента по стране: US → American и т. д., прочие — страна по-английски. */
fun accentOf(locale: Locale): String = when (locale.country.uppercase()) {
    "US" -> "American"
    "GB" -> "British"
    "AU" -> "Australian"
    "IN" -> "Indian"
    "" -> "English"
    else -> locale.getDisplayCountry(Locale.ENGLISH).ifBlank { locale.country }
}

/**
 * Группирует голоса по акценту: сначала американский, британский, австралийский, индийский,
 * потом остальные по алфавиту. Внутри группы — по имени, чтобы номера не прыгали между запусками.
 */
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
