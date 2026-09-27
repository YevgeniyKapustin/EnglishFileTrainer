package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.audio.AudioPlayerState
import dev.eftrainer.core.speech.SpeechRecognizerState
import dev.eftrainer.core.speech.rememberSpeechRecognizer
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.ui.explain.SendButton

@Composable
internal fun TalkField(
    enabled: Boolean,
    onSend: (String) -> Unit,
    onMic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val speech = rememberSpeechRecognizer { hypotheses -> hypotheses.firstOrNull()?.let { text = it } }
    val send = {
        if (enabled && text.isNotBlank()) {
            onSend(text.trim())
            text = ""
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                top = 6.dp,
            ).clip(shape)
            .background(C.Field)
            .border(1.5.dp, C.FieldBorder, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = T.field.copy(fontSize = 18.sp, lineHeight = 26.sp),
            cursorBrush = SolidColor(C.Accent),
            keyboardOptions = keyboard(ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) TalkPlaceholder(speech)
                inner()
            },
        )
        if (text.isBlank() && speech.available) {
            TalkMicButton(speech, enabled, onMic)
        } else {
            SendButton(enabled = enabled && text.isNotBlank(), onClick = send)
        }
    }
}

@Composable
private fun TalkPlaceholder(speech: SpeechRecognizerState) {
    Text(
        if (speech.listening) {
            speech.partial.ifEmpty { "…" }
        } else {
            stringResource(R.string.talk_placeholder)
        },
        style = T.field.copy(fontSize = 18.sp, color = C.Muted),
    )
}

@Composable
private fun TalkMicButton(
    speech: SpeechRecognizerState,
    enabled: Boolean,
    onMic: () -> Unit,
) {
    val background =
        when {
            speech.listening -> C.Bad
            enabled -> C.Accent
            else -> C.Disabled
        }
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(enabled = enabled) {
                if (speech.listening) {
                    speech.stop()
                } else {
                    onMic()
                    AudioPlayerState.pauseAll()
                    speech.start()
                }
            },
        contentAlignment = Alignment.Center,
    ) { Icon(MicIcon, stringResource(R.string.speak_mic), Modifier.size(20.dp), tint = C.OnAccent) }
}
