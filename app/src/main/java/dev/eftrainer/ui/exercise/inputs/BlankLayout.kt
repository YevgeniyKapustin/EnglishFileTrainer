package dev.eftrainer.ui.exercise.inputs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Task

@Composable
fun BlankLayout(
    task: Task,
    gaps: GapState,
    voice: TextToSpeechState?,
    modifier: Modifier = Modifier,
) {
    val e = gaps.exercise
    val verb = remember(e.id) { IrregularVerbs.find(e.id) }
    val keyWord = remember(e.id) { KeyWord.parse(e) }
    when {
        verb != null -> VerbCardInput(verb, gaps, voice, modifier)
        task.dialogue -> DialogueInput(gaps, voice, modifier)
        keyWord != null -> KeyWordInput(keyWord, gaps, voice, modifier)
        else -> BlankInput(gaps, voice, modifier)
    }
}
