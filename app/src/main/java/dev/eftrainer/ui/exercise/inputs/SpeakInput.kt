package dev.eftrainer.ui.exercise.inputs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.audio.AudioPlayerState
import dev.eftrainer.core.audio.rememberAudioPlayer
import dev.eftrainer.core.speech.SpeechError
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.speech.rememberSpeechRecognizer
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import dev.eftrainer.domain.check.Checker

/**
 * «Скажи вслух». Тап по микрофону → распознавание; из гипотез берётся та, что засчитывается
 * ([Checker.bestHypothesis]), кладётся в [value] и сразу проверяется.
 * Нет распознавателя, нет доступа к микрофону или сбой — ввод с клавиатуры (и «Type instead» всегда).
 */
@Composable
fun SpeakInput(
    exercise: Speak,
    attempt: Int,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    checked: Boolean,
    correct: Boolean,
    focus: FocusRequester,
    onAction: () -> Unit,
    tts: TextToSpeechState,
) {
    val sample = rememberAudioPlayer(exercise.audio)
    val act by rememberUpdatedState(onAction)
    var submit by remember(attempt) { mutableIntStateOf(0) }
    var heard by remember(attempt) { mutableStateOf<String?>(null) }
    var typed by remember(attempt) { mutableStateOf(false) }

    val speech = rememberSpeechRecognizer { hypotheses ->
        val best = Checker.bestHypothesis(exercise, hypotheses)
        heard = best
        onChange(TextFieldValue(best))
        submit++
    }
    // Проверяем после перерисовки: onAction из SessionScreen видит заполненное поле
    // только в следующей композиции (как setState → useEffect во фронте).
    LaunchedEffect(submit) { if (submit > 0) act() }
    LaunchedEffect(attempt) { speech.reset() }

    val error = speech.error
    val broken = error == SpeechError.NO_PERMISSION || error == SpeechError.UNAVAILABLE
    val typing = typed || broken || !speech.available
    LaunchedEffect(typing) { if (typing && !checked) focus.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Образец: трек книги, а если его нет — фраза синтезатором (нет и синтезатора — кнопки нет).
        val sayId = "speak-${exercise.id}"
        when {
            sample != null -> ListenButton(sample.playing, onClick = { tts.stop(); sample.toggle() })
            tts.available -> ListenButton(tts.speaking == sayId, onClick = {
                if (tts.speaking == sayId) tts.stop() else tts.speak(sayable(exercise.text), sayId)
            })
        }

        if (typing) {
            RewriteInput(Rewrite(exercise.id, exercise.text, exercise.answers, hint = exercise.hint), value, onChange, checked, correct, focus, onAction)
            if (!checked) {
                val note = when {
                    error == SpeechError.NO_PERMISSION -> R.string.speak_no_mic
                    broken || !speech.available -> R.string.speak_unavailable
                    else -> null
                }
                note?.let { Text(stringResource(it), style = T.label) }
                if (speech.available) {
                    Link(stringResource(R.string.speak_say_instead)) { typed = false; speech.reset() }
                }
            }
            return@Column
        }

        Text(exercise.text, style = T.question)
        exercise.hint?.let { Text(it, style = T.hint) }

        if (checked) {
            Heard(heard ?: value.text, correct)
            return@Column
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val pulse by animateFloatAsState(if (speech.listening) 1f + speech.level * 0.35f else 1f, label = "pulse")
            Box(contentAlignment = Alignment.Center) {
                if (speech.listening) {
                    Box(Modifier.size(80.dp).scale(pulse).clip(CircleShape).background(EF.Title.copy(alpha = 0.25f)))
                }
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(EF.Title)
                        .focusRequester(focus)
                        .focusable()
                        .clickable {
                            if (speech.listening) speech.stop()
                            else {
                                AudioPlayerState.pauseAll() // иначе микрофон услышит образец
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
            val partial = speech.partial
            Text(
                when {
                    speech.listening && partial.isNotBlank() -> partial
                    speech.listening -> stringResource(R.string.speak_listening)
                    error == SpeechError.NO_MATCH -> stringResource(R.string.speak_no_match)
                    else -> stringResource(R.string.speak_tap)
                },
                style = if (speech.listening && partial.isNotBlank()) T.field else T.hint,
                textAlign = TextAlign.Center,
                modifier = Modifier.heightIn(min = 28.dp),
            )
            Link(stringResource(R.string.speak_type_instead)) { typed = true; speech.cancel() }
        }
    }
}

/** Пилюля «Listen»: образец произношения (трек Student's Book или синтезатор). */
@Composable
private fun ListenButton(playing: Boolean, onClick: () -> Unit) {
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

/** Что распознано — после проверки, в рамке цвета результата. */
@Composable
private fun Heard(text: String, correct: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.speak_heard), style = T.label)
        val shape = RoundedCornerShape(14.dp)
        Text(
            text,
            style = T.field.copy(color = fieldColor(checked = true, wrong = !correct)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(C.Field)
                .border(1.5.dp, if (correct) C.Ok else C.Bad, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = T.label.copy(fontSize = 15.sp, color = EF.Title),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Группа букв читается по одной: "A K E" → "A. K. E." (иначе синтезатор читает «A» как артикль). */
private fun sayable(text: String): String =
    Checker.letterGroup(text)?.uppercase()?.toList()?.joinToString(". ", postfix = ".") ?: text
