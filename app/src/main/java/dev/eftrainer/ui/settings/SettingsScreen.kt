package dev.eftrainer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.speech.groupVoices
import dev.eftrainer.core.speech.rememberTextToSpeech
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.data.settings.SpeechRate
import dev.eftrainer.data.settings.VoiceSettings
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.exercise.inputs.SpeakerIcon

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = settingsViewModel(),
) {
    val mode by vm.theme.collectAsStateWithLifecycle()
    val sounds by vm.sounds.collectAsStateWithLifecycle()
    val soundVolume by vm.soundVolume.collectAsStateWithLifecycle()
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val reminderTime by vm.reminderTime.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        BackLink(stringResource(R.string.back), onBack, closesRight = true)
        Text(
            stringResource(R.string.settings),
            style = T.title,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            ThemeFold(mode, vm::setTheme)
            VoiceFold(vm.voice, onVoice)
            SoundsFold(sounds, soundVolume, vm::setSounds, vm::setSoundVolume, vm::sampleSound)
            ReminderFold(reminder, reminderTime, vm::setReminder, vm::setReminderTime)
        }
    }
}

@Composable
private fun settingsViewModel(): SettingsViewModel =
    appContainer().let { container -> viewModel { SettingsViewModel(container) } }

internal val SectionLabelStyle
    @Composable get() =
        TextStyle(
            fontFamily = Golos,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.06.em,
            color = EF.Title,
        )

@Composable
internal fun SectionLabel(text: String) {
    Text(text.uppercase(), style = SectionLabelStyle, modifier = Modifier.padding(top = 28.dp, bottom = 4.dp))
}

@Composable
private fun VoiceFold(
    voice: VoiceSettings,
    onVoice: () -> Unit,
) {
    val on by voice.readAloud.collectAsStateWithLifecycle()
    SettingsFold(stringResource(R.string.voice), EF.File2 to SpeakerIcon, summary = {
        AppSwitch(on, voice::setReadAloud)
    }) {
        val tts = rememberTextToSpeech()
        VoiceTuning(voice)
        VoicePickerRow(voice, tts, onVoice)
    }
}

@Composable
internal fun AppSwitch(
    on: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Switch(
        checked = on,
        onCheckedChange = onChange,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = C.OnAccent,
                checkedTrackColor = C.Accent,
                uncheckedThumbColor = C.Off,
                uncheckedTrackColor = C.Field,
                uncheckedBorderColor = C.FieldBorder,
            ),
    )
}

@Composable
private fun VoiceTuning(voice: VoiceSettings) {
    val volume by voice.volume.collectAsStateWithLifecycle()
    val rate by voice.rate.collectAsStateWithLifecycle()
    VolumeRow(volume, voice::setVolume, divider = true)
    ListRow(onClick = null, divider = true) {
        Text(stringResource(R.string.voice_speed), style = T.row, modifier = Modifier.weight(1f))
        Segmented(
            listOf(SpeechRate.NORMAL to R.string.voice_speed_normal, SpeechRate.SLOW to R.string.voice_speed_slow),
            selected = rate,
            onSelect = voice::setRate,
        )
    }
}

@Composable
private fun VoicePickerRow(
    voice: VoiceSettings,
    tts: TextToSpeechState,
    onVoice: () -> Unit,
) {
    val name by voice.voiceName.collectAsStateWithLifecycle()
    val current = name?.let { n -> groupVoices(tts.voices).flatMap { it.voices }.firstOrNull { it.name == n } }
    val known = name == null || tts.available
    ListRow(onClick = onVoice, divider = false) {
        Text(stringResource(R.string.voice), style = T.row, modifier = Modifier.weight(1f))
        Text(
            current?.let { stringResource(R.string.voice_current, it.accent, it.number) }
                ?: stringResource(R.string.voice_auto),
            style = T.rowSub,
            modifier = Modifier.alpha(if (known) 1f else 0f),
        )
        Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
    }
}

@Composable
private fun <V> Segmented(
    options: List<Pair<V, Int>>,
    selected: V,
    onSelect: (V) -> Unit,
) {
    Row(
        Modifier
            .clip(
                RoundedCornerShape(10.dp),
            ).background(C.Field)
            .border(1.dp, C.FieldBorder, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        options.forEach { (value, title) ->
            val on = value == selected
            Text(
                stringResource(title),
                style =
                    TextStyle(
                        fontFamily = Golos,
                        fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) C.OnAccent else C.Muted,
                    ),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) C.Accent else Color.Transparent)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun Tile(
    color: Color,
    icon: ImageVector,
) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color.White)
    }
}
