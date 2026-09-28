package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.IrregularVerb
import dev.eftrainer.ui.exercise.VerbSpeech

@Composable
fun VerbCardInput(
    verb: IrregularVerb,
    gaps: GapState,
    voice: TextToSpeechState?,
    modifier: Modifier = Modifier,
) {
    val labels = listOf(R.string.verb_past_simple, R.string.verb_past_participle).take(gaps.values.size)
    Column(
        modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EF.Panel)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Voiced(voice, VerbSpeech.prompt(verb), "question-${gaps.exercise.id}") {
            Text(
                verb.base,
                style = T.question.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            labels.forEachIndexed { i, label ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(label).uppercase(),
                        style =
                            T.label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.72.sp,
                                color = EF.Title,
                            ),
                    )
                    val last = i == labels.lastIndex
                    AnswerField(
                        value = gaps.values[i],
                        onChange = { gaps.onChange(i, it) },
                        checked = gaps.checked,
                        correct = i !in gaps.wrongGaps,
                        focus = gaps.focus[i],
                        onAction = { if (last || gaps.checked) gaps.onAction() else gaps.focus[i + 1].requestFocus() },
                        imeAction = if (last) ImeAction.Done else ImeAction.Next,
                        sentenceStart = false,
                    )
                }
            }
        }
    }
}
