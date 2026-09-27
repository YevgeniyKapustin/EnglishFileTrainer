package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Rewrite

/** «Напиши предложение целиком»; с prefill — «исправь ошибку» прямо в поле. */
@Composable
fun RewriteInput(
    exercise: Rewrite,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    checked: Boolean,
    correct: Boolean,
    focus: FocusRequester,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (exercise.prefill == null) Text(exercise.prompt, style = T.question)
        exercise.hint?.let { Text(it, style = T.hint) }

        val border = when {
            !checked -> C.FieldBorder
            correct -> C.Ok
            else -> C.Bad
        }
        val shape = RoundedCornerShape(14.dp)
        BasicTextField(
            value = value,
            onValueChange = { if (!checked) onChange(it) },
            readOnly = checked,
            textStyle = T.field.copy(color = fieldColor(checked, !correct)),
            cursorBrush = SolidColor(C.Accent),
            keyboardOptions = keyboard(ImeAction.Done),
            keyboardActions = actions(onAction),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(focus),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(C.Field)
                        .border(1.5.dp, border, shape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) { inner() }
            },
        )
    }
}
