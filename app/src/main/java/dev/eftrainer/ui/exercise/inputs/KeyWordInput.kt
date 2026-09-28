package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.T

@Composable
fun KeyWordInput(
    item: KeyWord,
    gaps: GapState,
    voice: TextToSpeechState?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Voiced(voice, QuestionSpeech.text(item.key), "question-${gaps.exercise.id}") {
            Text(
                item.key,
                style = T.question.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        gaps.exercise.hint?.let { Text(it, style = T.hint) }
        if (item.options.isNotEmpty()) WordBox(item.options)
        AnswerField(
            value = gaps.values[0],
            onChange = { gaps.onChange(0, it) },
            checked = gaps.checked,
            correct = 0 !in gaps.wrongGaps,
            focus = gaps.focus[0],
            onAction = gaps.onAction,
            modifier = Modifier.padding(top = 4.dp),
            sentenceStart = Capitals.gapStarts(gaps.exercise.text).firstOrNull() ?: true,
        )
    }
}
