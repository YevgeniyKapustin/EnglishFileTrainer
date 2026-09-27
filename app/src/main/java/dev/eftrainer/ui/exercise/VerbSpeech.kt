package dev.eftrainer.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.data.content.IrregularVerb
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.ui.appContainer
import kotlinx.coroutines.delay

object VerbSpeech {
    const val GAP_AFTER_CUE_MILLIS = 300L

    fun prompt(verb: IrregularVerb): String = verb.base

    fun forms(verb: IrregularVerb): String =
        listOfNotNull(verb.base, verb.pastSimple.firstOrNull(), verb.pastParticiple.firstOrNull())
            .joinToString(", ") { form -> form.split("/").joinToString(" or ") { it.trim() } }

    fun delayAfterCheck(cue: Cue?): Long = cue?.let { it.millis + GAP_AFTER_CUE_MILLIS } ?: 0L
}

@Composable
internal fun VerbVoice(
    q: SessionUi.Question,
    tts: TextToSpeechState,
) {
    val verb = remember(q.exercise.id) { IrregularVerbs.find(q.exercise.id) } ?: return
    val container = appContainer()
    val readAloud by container.voice.readAloud.collectAsStateWithLifecycle()
    if (!readAloud) return
    val feedback = q.feedback
    val sayId = "verb-${verb.base}-${q.attempt}"
    LaunchedEffect(sayId, feedback != null) {
        if (feedback == null) {
            tts.speak(VerbSpeech.prompt(verb), sayId)
            return@LaunchedEffect
        }
        tts.stop()
        val cue = Cue.forAnswer(feedback.correct).takeIf { container.sounds.audible() }
        delay(VerbSpeech.delayAfterCheck(cue))
        tts.speak(VerbSpeech.forms(verb), sayId)
    }
}
