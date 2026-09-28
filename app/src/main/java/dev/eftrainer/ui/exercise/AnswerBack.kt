package dev.eftrainer.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.linkClickable
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.domain.ai.AnswerBackPrompt
import dev.eftrainer.domain.ai.WritePrompt
import dev.eftrainer.ui.exercise.inputs.AnswerField
import dev.eftrainer.ui.explain.LinkText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class AnswerBack(
    val question: String,
    val check: AiCheck? = null,
    val grade: WritePrompt.Grade? = null,
) {
    val canCheck: Boolean get() = grade == null && check != AiCheck.Checking
    val settled: Boolean get() = grade != null || check is AiCheck.Failed
}

class AnswerBackChecks(
    private val state: MutableStateFlow<SessionUi>,
    private val scope: CoroutineScope,
    private val client: AiClient,
) {
    fun check(text: String) {
        val q = state.value as? SessionUi.Question ?: return
        val pending = q.answerBack?.takeIf { it.canCheck && text.isNotBlank() } ?: return
        state.value = q.copy(answerBack = pending.copy(check = AiCheck.Checking))
        scope.launch {
            val graded = pending.graded(client, text)
            val now = state.value as? SessionUi.Question ?: return@launch
            if (now.attempt == q.attempt) state.value = now.copy(answerBack = graded)
        }
    }
}

private suspend fun AnswerBack.graded(
    client: AiClient,
    answer: String,
): AnswerBack {
    val prompt = AnswerBackPrompt.grading(question, answer)
    return try {
        val grade =
            AnswerBackPrompt.parseGrade(client.chat(prompt))
                ?: AnswerBackPrompt.parseGrade(client.chat(prompt))
                ?: WritePrompt.Grade(pass = false, summary = "", corrections = emptyList())
        copy(check = null, grade = grade)
    } catch (e: AiException) {
        copy(check = AiCheck.Failed(e.reason))
    }
}

@Composable
internal fun AnswerBackBlock(
    answerBack: AnswerBack,
    onCheck: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember(answerBack.question) { mutableStateOf(TextFieldValue()) }
    val focus = remember { FocusRequester() }
    val grade = answerBack.grade
    val send = { if (answerBack.canCheck && value.text.isNotBlank()) onCheck(value.text) }
    val shown = remember { BringIntoViewRequester() }
    LaunchedEffect(answerBack) { shown.bringIntoView() }
    Column(
        modifier.bringIntoViewRequester(shown).padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.answer_back),
            style = T.label.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
        Text(answerBack.question, style = T.rowEn.copy(fontSize = 17.sp, lineHeight = 24.sp))
        AnswerField(
            value = value,
            onChange = { value = it },
            checked = grade != null,
            correct = grade?.pass == true,
            focus = focus,
            onAction = send,
        )
        when {
            grade != null -> {
                AiGradedFeedback(grade.summary, grade.corrections)
            }

            answerBack.check == AiCheck.Checking -> {
                AnswerBackStatus(stringResource(R.string.write_checking))
            }

            answerBack.check is AiCheck.Failed -> {
                AnswerBackStatus(
                    stringResource(aiErrorRes(answerBack.check.reason)),
                    onRetry = send,
                )
            }

            else -> {
                LinkText(stringResource(R.string.check), onClick = send, enabled = value.text.isNotBlank())
            }
        }
    }
}

@Composable
private fun AnswerBackStatus(
    text: String,
    onRetry: (() -> Unit)? = null,
) {
    val style = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp)
    if (onRetry == null) {
        Text(text, style = style)
    } else {
        Text(
            text,
            style = style.copy(color = C.Bad),
            modifier =
                Modifier
                    .linkClickable(onClick = onRetry)
                    .padding(vertical = 4.dp),
        )
    }
}
