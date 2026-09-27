package dev.eftrainer.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.eftrainer.R
import dev.eftrainer.core.speech.groupVoices
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.exercise.inputs.rememberVoice

@Composable
fun VoicePickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = appContainer().voice
    val tts = rememberVoice()
    val selected by settings.voiceName.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val preview = stringResource(R.string.voice_preview)
    val groups = remember(tts.voices) { groupVoices(tts.voices) }
    val current = selected?.takeIf { n -> groups.any { g -> g.voices.any { it.name == n } } }
    val more = remember(tts.available) { moreVoicesIntent(context, tts.engine) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tts.refreshVoices() }

    fun choose(name: String?) {
        settings.setVoiceName(name)
        tts.voiceName = name
        tts.speak(preview, "voice-preview")
    }

    Column(modifier.fillMaxSize()) {
        BackLink(stringResource(R.string.settings), onBack)
        Text(
            stringResource(R.string.voice),
            style = T.title,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            ListRow(onClick = { choose(null) }, divider = false) {
                Text(stringResource(R.string.voice_auto), style = T.row, modifier = Modifier.weight(1f))
                Check(current == null)
            }
            Column(Modifier.animateContentSize()) {
                groups.forEach { group ->
                    SectionLabel(group.accent)
                    group.voices.forEachIndexed { i, v ->
                        ListRow(onClick = { choose(v.name) }, divider = i != group.voices.lastIndex) {
                            Text(
                                stringResource(R.string.voice_n, v.number),
                                style = T.row,
                                modifier = Modifier.weight(1f),
                            )
                            Check(v.name == current)
                        }
                    }
                }
            }
            if (more != null) {
                Column(Modifier.padding(top = 20.dp)) {
                    ListRow(onClick = { open(context, more) }, divider = false) {
                        Text(stringResource(R.string.voice_more), style = T.row, modifier = Modifier.weight(1f))
                        Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
                    }
                }
            }
        }
    }
}

@Composable
private fun Check(on: Boolean) {
    Icon(Icons.Check, null, Modifier.size(20.dp).alpha(if (on) 1f else 0f), tint = EF.Teal)
}

private fun moreVoicesIntent(
    context: Context,
    engine: String?,
): Intent? {
    val pm = context.packageManager
    val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply { engine?.let { setPackage(it) } }
    val settings = Intent("com.android.settings.TTS_SETTINGS")
    return listOf(install, settings).firstOrNull { it.resolveActivity(pm) != null }
}

private fun open(
    context: Context,
    intent: Intent,
) {
    try {
        context.startActivity(intent)
    } catch (ignored: ActivityNotFoundException) {
    } catch (ignored: SecurityException) {
    }
}
