package dev.eftrainer.domain.check

import dev.eftrainer.core.audio.AudioAssets
import dev.eftrainer.data.content.Speak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakCheckTest {

    private val question = Speak("t-speak", "What's your name?", listOf("What's your name?"))
    private val letters = Speak("t-abc", "A H J K", listOf("A H J K"))
    private val number = Speak("t-num", "I'm 21.", listOf("I'm 21."))
    private val date = Speak("t-date", "It's on the 3rd of May.", listOf("It's on the 3rd of May."))

    private fun ok(e: Speak, said: String) = Checker.check(e, said).correct

    @Test fun `case and punctuation are ignored`() = assertTrue(ok(question, "what's your name"))

    @Test fun `contractions match full forms`() {
        assertTrue(ok(question, "What is your name"))
        assertTrue(ok(Speak("t", "I don't know.", listOf("I don't know.")), "I do not know"))
        assertTrue(ok(Speak("t", "I can't swim.", listOf("I can't swim.")), "I cannot swim"))
    }

    @Test fun `typographic apostrophe`() = assertTrue(ok(question, "What’s your name?"))

    @Test fun `digits match words`() {
        assertTrue(ok(number, "I am twenty-one"))
        assertTrue(ok(number, "I'm twenty one"))
        assertTrue(ok(date, "it's on the third of May"))
        assertTrue(ok(Speak("t", "at 7:30", listOf("at seven thirty")), "At 7:30."))
    }

    @Test fun `spelled letters match however split`() {
        assertTrue(ok(letters, "a h j k"))
        assertTrue(ok(letters, "AHJK"))
        assertTrue(ok(letters, "A-H-J-K"))
        assertTrue(ok(letters, "A. H. J. K."))
    }

    @Test fun `wrong words fail`() {
        assertFalse(ok(question, "What's his name?"))
        assertFalse(ok(letters, "a h j"))
    }

    @Test fun `single letter words stay words`() =
        assertEquals("i am a student", Checker.words("I'm a student."))

    @Test fun `expected is the phrase`() = assertEquals("What's your name?", Checker.check(question, "x").expected)

    @Test fun `best hypothesis prefers a correct alternate`() =
        assertEquals("What's your name", Checker.bestHypothesis(question, listOf("What's your game", "What's your name")))

    @Test fun `best hypothesis falls back to the first`() =
        assertEquals("one", Checker.bestHypothesis(question, listOf("one", "two")))

    @Test fun `best hypothesis of nothing is empty`() = assertEquals("", Checker.bestHypothesis(question, emptyList()))

    @Test fun `ordinals`() {
        assertEquals("twenty first", Checker.words("21st"))
        assertEquals("twelfth", Checker.words("12th"))
        assertEquals("fortieth", Checker.words("40th"))
    }

    @Test fun `audio asset names`() =
        assertEquals(listOf("audio/1_9.ogg", "audio/1_9.mp3"), AudioAssets.candidates("1.9"))
}
