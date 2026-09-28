package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.linkClickable
import dev.eftrainer.data.content.Write
import dev.eftrainer.data.content.WriteQuestion
import dev.eftrainer.ui.exercise.AiCheck
import dev.eftrainer.ui.exercise.aiErrorRes

@Composable
fun WriteInput(
    exercise: Write,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    checked: Boolean,
    focus: FocusRequester,
    aiCheck: AiCheck?,
    onRetry: () -> Unit,
    voice: TextToSpeechState?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Voiced(voice, QuestionSpeech.lines(listOf(exercise.prompt) + exercise.questions), "question-${exercise.id}") {
            Text(exercise.prompt, style = T.question.copy(fontSize = 19.sp, lineHeight = 28.sp))
        }
        if (exercise.questions.isNotEmpty()) WriteQuestions(exercise.questions)
        exercise.hint?.let { Text(it, style = T.hint) }
        WriteField(
            value = value,
            onChange = onChange,
            locked = checked || aiCheck == AiCheck.Checking,
            focus = focus,
        )
        AiCheckStatus(aiCheck, onRetry)
    }
}

@Composable
private fun WriteQuestions(questions: List<WriteQuestion>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        questions.forEachIndexed { i, q ->
            Row {
                Text(
                    "${i + 1}",
                    style = T.rowEn.copy(color = EF.Title, fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(26.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(q.text, style = T.rowEn.copy(fontSize = 17.sp, lineHeight = 24.sp))
                    Text(q.hint, style = T.hint)
                }
            }
        }
    }
}

@Composable
private fun WriteField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    locked: Boolean,
    focus: FocusRequester,
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = { if (!locked) onChange(it.copy(text = Capitals.fix(it.text, sentenceStart = true))) },
        readOnly = locked,
        textStyle = T.field.copy(fontSize = 18.sp, lineHeight = 27.sp, color = C.Ink),
        cursorBrush = SolidColor(C.Accent),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            ),
        minLines = 6,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).focusRequester(focus),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .clip(shape)
                    .background(C.Field)
                    .border(1.5.dp, C.FieldBorder, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        stringResource(R.string.write_placeholder),
                        style = T.field.copy(fontSize = 18.sp, color = C.Muted),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun AiCheckStatus(
    aiCheck: AiCheck?,
    onRetry: () -> Unit,
) {
    val small = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp)
    when (aiCheck) {
        AiCheck.Checking -> {
            Text(stringResource(R.string.write_checking), style = small)
        }

        is AiCheck.Failed -> {
            Text(
                stringResource(aiErrorRes(aiCheck.reason)),
                style = small.copy(color = C.Bad),
                modifier =
                    Modifier
                        .linkClickable(onClick = onRetry)
                        .padding(vertical = 4.dp),
            )
        }

        null -> {}
    }
}
