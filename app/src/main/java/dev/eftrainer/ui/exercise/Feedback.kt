package dev.eftrainer.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.ui.explain.ChatThread
import dev.eftrainer.ui.exercise.inputs.SayIcon
import dev.eftrainer.core.speech.TextToSpeechState
import androidx.compose.foundation.layout.offset
import dev.eftrainer.ui.explain.LinkText
import dev.eftrainer.ui.explain.Selectable

// Нижняя панель после проверки: верно / правильный ответ / «Why?» (ИИ) с уточняющими вопросами.
// У разговора (Talk) и письменного ответа (Write) вместо правильного ответа — итог от ИИ,
// у Write ещё исправления «ошибка → как правильно».

@Composable
internal fun FeedbackSheet(
    f: Feedback,
    explanation: Explanation?,
    aiOn: Boolean,
    aiGraded: Boolean,
    onNext: () -> Unit,
    onExplain: (question: String?) -> Unit,
    tts: TextToSpeechState,
    sayId: String,
) {
    val color = if (f.correct) C.Ok else C.Bad
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(if (f.correct) C.OkBg else C.BadBg)
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (f.correct) Icon(Icons.Check, null, Modifier.size(20.dp), tint = color)
            val title = when {
                aiGraded -> if (f.correct) R.string.talk_passed else R.string.talk_not_yet
                f.correct -> R.string.correct
                else -> R.string.correct_answer
            }
            Text(stringResource(title), style = T.row.copy(color = color, fontWeight = FontWeight.SemiBold))
            // Верно — динамик прямо у «Correct»: прочитать правильное предложение целиком.
            if (f.correct && !aiGraded) SayIcon(tts, f.expected, sayId, Modifier.offset(x = (-6).dp))
        }
        if (aiGraded) {
            // Итог от ИИ: что получилось и главные ошибки — обычным текстом, не «ответом».
            Selectable {
                Column(
                    Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(f.expected.ifEmpty { stringResource(R.string.write_ungraded) }, style = T.body.copy(fontSize = 15.sp, lineHeight = 22.sp))
                    f.corrections.forEach { c ->
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = C.Bad, textDecoration = TextDecoration.LineThrough)) { append(c.mistake) }
                                append(" → ")
                                withStyle(SpanStyle(color = C.Ok)) { append(c.correction) }
                            },
                            style = T.rowEn.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        )
                    }
                }
            }
        } else if (!f.correct) {
            Selectable {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.expected, style = T.field, modifier = Modifier.weight(1f, fill = false))
                        SayIcon(tts, f.expected, sayId)
                    }
                    f.explain?.let { Text(it, style = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp)) }
                }
            }
            // Уже полученное объяснение не прячем, даже если сеть пропала.
            if (aiOn || explanation?.answered == true) AiExplanation(explanation, aiOn, onExplain)
        }
        PrimaryButton(stringResource(R.string.continue_), onClick = onNext, color = color, modifier = Modifier.padding(top = 8.dp))
    }
}

/** «Why?» → объяснение ИИ и мини-чат. Появляется, только если ИИ включён (ключ + сеть). */
@Composable
private fun AiExplanation(explanation: Explanation?, aiOn: Boolean, onExplain: (String?) -> Unit) {
    if (explanation == null) LinkText(stringResource(R.string.why), onClick = { onExplain(null) })
    else ChatThread(explanation, canAsk = aiOn, onAsk = { onExplain(it) }, onRetry = { onExplain(null) }, maxHeight = 220.dp)
}

/** Ошибка ИИ → строка для ученика. Общая для «Why?», «Explain» и разговора. */
fun aiErrorRes(reason: AiException.Reason): Int = when (reason) {
    AiException.Reason.BAD_KEY, AiException.Reason.NO_KEY -> R.string.ai_bad_key
    AiException.Reason.NETWORK -> R.string.ai_no_connection
    AiException.Reason.SERVER -> R.string.ai_failed
}
