package dev.eftrainer.core.speech

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class VoicesTest {

    @Test
    fun groupsByAccentWithStableNumbers() {
        val groups = groupVoices(
            listOf(
                VoiceInfo("en-gb-x-rjs-local", Locale.UK),
                VoiceInfo("en-ng-x-tfn-local", Locale("en", "NG")),
                VoiceInfo("en-us-x-tpf-local", Locale.US),
                VoiceInfo("en-au-x-aua-local", Locale("en", "AU")),
                VoiceInfo("en-us-x-iol-local", Locale.US),
                VoiceInfo("en-us-x-iol-local", Locale.US),
            ),
        )
        assertEquals(listOf("American", "British", "Australian", "Nigeria"), groups.map { it.accent })
        assertEquals(listOf("en-us-x-iol-local" to 1, "en-us-x-tpf-local" to 2), groups[0].voices.map { it.name to it.number })
    }
}
