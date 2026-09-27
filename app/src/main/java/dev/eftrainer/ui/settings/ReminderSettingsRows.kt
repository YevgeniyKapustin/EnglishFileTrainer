package dev.eftrainer.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.reminder.Reminders
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
internal fun ReminderRows(
    on: Boolean,
    time: LocalTime,
    onToggle: (Boolean) -> Unit,
    onTime: (LocalTime) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun toggle(value: Boolean) {
        onToggle(value)
        if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canPost(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ListRow(onClick = { toggle(!on) }, divider = on) {
        Tile(EF.Teal, Icons.Bell)
        Text(stringResource(R.string.reminder), style = T.row, modifier = Modifier.weight(1f))
        AppSwitch(on, ::toggle)
    }
    if (on) {
        ListRow(onClick = { picking = true }, divider = false) {
            Text(stringResource(R.string.reminder_time), style = T.row, modifier = Modifier.weight(1f))
            Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), style = T.rowSub)
            Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
        }
    }
    if (picking) {
        TimeDialog(
            time = time,
            onDismiss = { picking = false },
            onPick = {
                picking = false
                onTime(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    time: LocalTime,
    onDismiss: () -> Unit,
    onPick: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Paper,
        text = {
            TimePicker(
                state,
                colors =
                    TimePickerDefaults.colors(
                        selectorColor = C.Accent,
                        timeSelectorSelectedContainerColor = C.Accent,
                        timeSelectorSelectedContentColor = C.OnAccent,
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.ok), color = C.Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = C.Muted) }
        },
    )
}
