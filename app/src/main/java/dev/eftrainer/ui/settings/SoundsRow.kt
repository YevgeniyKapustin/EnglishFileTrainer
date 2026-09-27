package dev.eftrainer.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.eftrainer.R
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow

@Composable
internal fun SoundsRow(
    on: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListRow(onClick = { onChange(!on) }, divider = false) {
        Tile(EF.Navy, Icons.Note)
        Text(stringResource(R.string.sound_effects), style = T.row, modifier = Modifier.weight(1f))
        AppSwitch(on, onChange)
    }
}
