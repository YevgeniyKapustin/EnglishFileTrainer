package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.speech.rememberTextToSpeech
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.ui.appContainer

private val LocalVoice = staticCompositionLocalOf<TextToSpeechState?> { null }

@Composable
fun ProvideVoice(content: @Composable () -> Unit) {
    val voice = appContainer().voice
    val tts = rememberTextToSpeech()
    val volume by voice.volume.collectAsStateWithLifecycle()
    val rate by voice.rate.collectAsStateWithLifecycle()
    val name by voice.voiceName.collectAsStateWithLifecycle()
    tts.volume = volume
    tts.rate = rate.factor
    SideEffect { tts.voiceName = name }
    CompositionLocalProvider(LocalVoice provides tts, content)
}

@Composable
fun rememberVoice(): TextToSpeechState {
    val tts = checkNotNull(LocalVoice.current) { "rememberVoice needs ProvideVoice above it" }
    DisposableEffect(tts) { onDispose { tts.stop() } }
    return tts
}

@Composable
fun SayIcon(
    tts: TextToSpeechState,
    text: String,
    id: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    if (!tts.available || text.isBlank()) return
    val speaking = tts.speaking == id
    Box(
        modifier
            .size(size + 16.dp)
            .clip(CircleShape)
            .clickable { if (speaking) tts.stop() else tts.speak(text, id) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            SpeakerIcon,
            stringResource(if (speaking) R.string.talk_stop_reading else R.string.talk_read_aloud),
            Modifier.size(size),
            tint = if (speaking) EF.Title else C.Muted,
        )
    }
}
