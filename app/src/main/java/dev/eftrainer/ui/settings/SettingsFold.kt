package dev.eftrainer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow

private const val OPEN_TURN_DEGREES = 90f

@Composable
internal fun SettingsFold(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) OPEN_TURN_DEGREES else 0f, label = "fold")
    Column(modifier) {
        ListRow(onClick = { open = !open }, divider = false) {
            Text(title.uppercase(), style = SectionLabelStyle, modifier = Modifier.weight(1f))
            Icon(Icons.ChevronRight, null, Modifier.size(20.dp).rotate(turn), tint = C.Chevron)
        }
        AnimatedVisibility(
            open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(bottom = 8.dp), content = content)
        }
        HorizontalDivider(color = C.Line)
    }
}
