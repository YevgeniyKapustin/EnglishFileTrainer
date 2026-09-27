package dev.eftrainer.ui.exercise.inputs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Task

@Composable
fun BlankLayout(
    task: Task,
    gaps: GapState,
    modifier: Modifier = Modifier,
) {
    val e = gaps.exercise
    val verb = remember(e.id) { IrregularVerbs.find(e.id) }
    val keyWord = remember(e.id) { KeyWord.parse(e) }
    when {
        verb != null -> VerbCardInput(verb, gaps, modifier)
        task.dialogue -> DialogueInput(gaps, modifier)
        keyWord != null -> KeyWordInput(keyWord, gaps, modifier)
        else -> BlankInput(gaps, modifier)
    }
}
