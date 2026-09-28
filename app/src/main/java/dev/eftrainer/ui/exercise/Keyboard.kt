package dev.eftrainer.ui.exercise

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset

@Composable
internal fun HideKeyboardWhen(hidden: Boolean) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(hidden) { if (hidden) keyboard?.hide() }
}

@Composable
internal fun Modifier.belowKeyboardUnless(typing: Boolean): Modifier = if (typing) this else belowKeyboard()

@Composable
internal fun Modifier.belowKeyboard(): Modifier {
    val ime = WindowInsets.ime
    val bars = WindowInsets.navigationBars
    return offset { IntOffset(0, (ime.getBottom(this) - bars.getBottom(this)).coerceAtLeast(0)) }
}
