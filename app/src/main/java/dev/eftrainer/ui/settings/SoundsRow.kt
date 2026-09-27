package dev.eftrainer.ui.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.ListRow

@Composable
internal fun VolumeRow(
    volume: Float,
    onChange: (Float) -> Unit,
    divider: Boolean,
    onSettle: () -> Unit = {},
) {
    ListRow(onClick = null, divider = divider) {
        Text(stringResource(R.string.voice_volume), style = T.row)
        Slider(
            value = volume,
            onValueChange = onChange,
            onValueChangeFinished = onSettle,
            modifier = Modifier.weight(1f),
            colors =
                SliderDefaults.colors(
                    thumbColor = C.Accent,
                    activeTrackColor = C.Accent,
                    inactiveTrackColor = C.Line,
                ),
        )
        Text("${(volume * 100).toInt()}%", style = T.rowSub, modifier = Modifier.widthIn(min = 40.dp))
    }
}

@Composable
internal fun SoundsFold(
    on: Boolean,
    volume: Float,
    onToggle: (Boolean) -> Unit,
    onVolume: (Float) -> Unit,
    onSettle: () -> Unit,
) {
    SettingsFold(stringResource(R.string.sounds), summary = { AppSwitch(on, onToggle) }) {
        VolumeRow(volume, onVolume, divider = false, onSettle = onSettle)
    }
}
