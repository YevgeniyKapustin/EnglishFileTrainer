package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.boldMarkup
import dev.eftrainer.data.content.Rewrite

@Composable
fun RewriteInput(
    exercise: Rewrite,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    checked: Boolean,
    correct: Boolean,
    focus: FocusRequester,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val lines = exercise.prompt.lines().filter { it.isNotBlank() }
        if (lines.size > 1) Options(lines) else Text(boldMarkup(exercise.prompt), style = T.question)
        exercise.hint?.let { Text(it, style = T.hint) }
        AnswerField(
            value = value,
            onChange = onChange,
            checked = checked,
            correct = correct,
            focus = focus,
            onAction = onAction,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Options(lines: List<String>) {
    val style = T.question.copy(fontSize = 19.sp, lineHeight = 28.sp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEachIndexed { i, line ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    ('A' + i).toString(),
                    style = style.copy(color = EF.Title, fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(14.dp),
                )
                Text(boldMarkup(line), style = style, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AnswerField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    checked: Boolean,
    correct: Boolean,
    focus: FocusRequester,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
) {
    val border =
        when {
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
        keyboardOptions = keyboard(imeAction),
        keyboardActions = actions(onAction),
        modifier = modifier.fillMaxWidth().focusRequester(focus),
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
