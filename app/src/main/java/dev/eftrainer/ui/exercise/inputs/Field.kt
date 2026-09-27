package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.eftrainer.core.theme.C

/** Состояние поля после проверки: null — ещё не проверено. */
internal fun fieldColor(checked: Boolean, wrong: Boolean): Color = when {
    !checked -> C.Ink
    wrong -> C.Bad
    else -> C.Ok
}

/** Без автозамены: иначе клавиатура «исправит» ответ, и проверка станет нечестной. */
internal fun keyboard(ime: ImeAction) = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Text,
    imeAction = ime,
)

internal fun actions(onAction: () -> Unit) = KeyboardActions(onDone = { onAction() }, onNext = { onAction() })
