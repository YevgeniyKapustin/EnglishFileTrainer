package dev.eftrainer.ui.exercise.inputs

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionSpeechTest {
    @Test
    fun gapsBecomePauses() {
        assertEquals("I … to school yesterday.", QuestionSpeech.text("I {} to school yesterday."))
    }

    @Test
    fun linesEndAsSentences() {
        assertEquals("Where were you? I was at home.", QuestionSpeech.text("Where were you?\nI was at home"))
    }

    @Test
    fun boldMarksAreNotRead() {
        assertEquals("She is tall.", QuestionSpeech.text("She **is** tall."))
    }

    @Test
    fun blankLinesAreSkipped() {
        assertEquals("Hello. Bye.", QuestionSpeech.lines(listOf("Hello", "  ", "Bye")))
    }
}
