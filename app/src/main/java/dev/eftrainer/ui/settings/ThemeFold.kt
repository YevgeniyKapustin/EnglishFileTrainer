package dev.eftrainer.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.eftrainer.R
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.data.settings.ThemeMode

private val ThemeOptions =
    listOf(
        Triple(ThemeMode.SYSTEM, R.string.theme_system, EF.Teal to Icons.HalfCircle),
        Triple(ThemeMode.LIGHT, R.string.theme_light, Color(0xFFF2A93B) to Icons.Sun),
        Triple(ThemeMode.DARK, R.string.theme_dark, EF.Navy to Icons.Moon),
    )

@Composable
internal fun ThemeFold(
    mode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val current = ThemeOptions.first { it.first == mode }
    SettingsFold(
        stringResource(R.string.theme),
        summary = { Text(stringResource(current.second), style = T.rowSub) },
    ) { close ->
        val others = ThemeOptions - current
        others.forEachIndexed { i, (option, title, tile) ->
            ListRow(
                onClick = {
                    onSelect(option)
                    close()
                },
                divider = i != others.lastIndex,
            ) {
                Tile(tile.first, tile.second)
                Text(stringResource(title), style = T.row, modifier = Modifier.weight(1f))
            }
        }
    }
}
