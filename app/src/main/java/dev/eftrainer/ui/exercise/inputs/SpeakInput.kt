package dev.eftrainer.ui.exercise.inputs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.audio.AudioPlayerState
import dev.eftrainer.core.audio.rememberAudioPlayer
import dev.eftrainer.core.speech.SpeechError
import dev.eftrainer.core.speech.SpeechRecognizerState
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.speech.rememberSpeechRecognizer
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Speak
import dev.eftrainer.domain.check.Checker

@Composable
fun SpeakInput(
    exercise: Speak,
    attempt: Int,
    onHear: (String) -> Unit,
    checked: Boolean,
    correct: Boolean,
    tts: TextToSpeechState,
    bookAudio: Boolean,
    modifier: Modifier = Modifier,
) {
    val sample = rememberAudioPlayer(exercise.audio)
    val heardNow by rememberUpdatedState(onHear)
    var heard by remember(attempt) { mutableStateOf<String?>(null) }

    val speech =
        rememberSpeechRecognizer { hypotheses ->
            val best = Checker.bestHypothesis(exercise, hypotheses)
            heard = best
            heardNow(best)
        }
    LaunchedEffect(attempt) { speech.reset() }

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SampleButton(exercise, sample, tts.takeUnless { bookAudio })
        Text(exercise.text, style = T.question.copy(textAlign = TextAlign.Center))
        exercise.hint?.let { Text(it, style = T.hint.copy(textAlign = TextAlign.Center)) }
        if (checked) {
            Heard(heard.orEmpty(), correct)
        } else {
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val note = unavailableNote(speech)
                if (note == null) {
                    MicButton(speech, tts)
                    SpeechStatus(speech)
                } else {
                    Text(stringResource(note), style = T.hint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private val SpeechRecognizerState.broken: Boolean
    get() = error == SpeechError.NO_PERMISSION || error == SpeechError.UNAVAILABLE

private fun unavailableNote(speech: SpeechRecognizerState): Int? =
    when {
        speech.error == SpeechError.NO_PERMISSION -> R.string.speak_no_mic
        speech.broken || !speech.available -> R.string.speak_unavailable
        else -> null
    }

@Composable
private fun SampleButton(
    exercise: Speak,
    sample: AudioPlayerState?,
    tts: TextToSpeechState?,
) {
    val sayId = "speak-${exercise.id}"
    when {
        sample != null -> {
            ListenButton(sample.playing, onClick = {
                tts?.stop()
                sample.toggle()
            })
        }

        tts != null && tts.available -> {
            ListenButton(tts.speaking == sayId, onClick = {
                if (tts.speaking == sayId) tts.stop() else tts.speak(sayable(exercise.text), sayId)
            })
        }
    }
}

@Composable
private fun MicButton(
    speech: SpeechRecognizerState,
    tts: TextToSpeechState,
) {
    val pulse by animateFloatAsState(if (speech.listening) 1f + speech.level * 0.35f else 1f, label = "pulse")
    Box(contentAlignment = Alignment.Center) {
        if (speech.listening) {
            Box(
                Modifier
                    .size(80.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(EF.Title.copy(alpha = 0.25f)),
            )
        }
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(EF.Title)
                .clickable {
                    if (speech.listening) {
                        speech.stop()
                    } else {
                        AudioPlayerState.pauseAll()
                        tts.stop()
                        speech.start()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                MicIcon,
                stringResource(if (speech.listening) R.string.speak_stop else R.string.speak_mic),
                Modifier.size(34.dp),
                tint = EF.TagInk,
            )
        }
    }
}

@Composable
private fun SpeechStatus(speech: SpeechRecognizerState) {
    val partial = speech.partial
    val showPartial = speech.listening && partial.isNotBlank()
    Text(
        when {
            showPartial -> partial
            speech.listening -> stringResource(R.string.speak_listening)
            speech.error == SpeechError.NO_MATCH -> stringResource(R.string.speak_no_match)
            else -> stringResource(R.string.speak_tap)
        },
        style = if (showPartial) T.field else T.hint,
        textAlign = TextAlign.Center,
        modifier = Modifier.heightIn(min = 28.dp),
    )
}

@Composable
private fun ListenButton(
    playing: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .clip(shape)
            .border(1.dp, C.Line, shape)
            .background(C.Field)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(SpeakerIcon, null, Modifier.size(20.dp), tint = EF.Title)
        Text(
            stringResource(if (playing) R.string.audio_pause else R.string.speak_listen),
            style = T.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EF.Title),
        )
    }
}

@Composable
private fun Heard(
    text: String,
    correct: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.speak_heard), style = T.label)
        val shape = RoundedCornerShape(14.dp)
        Text(
            text,
            style = T.field.copy(color = fieldColor(checked = true, wrong = !correct)),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(C.Field)
                    .border(1.5.dp, if (correct) C.Ok else C.Bad, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

private fun sayable(text: String): String =
    Checker
        .letterGroup(text)
        ?.uppercase()
        ?.toList()
        ?.joinToString(". ", postfix = ".") ?: text
