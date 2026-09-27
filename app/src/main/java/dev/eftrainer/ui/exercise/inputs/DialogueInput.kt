package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.theme.T

internal fun dialogueLines(tokens: List<Token>): List<List<Token>> =
    tokens
        .fold(mutableListOf(mutableListOf<Token>())) { lines, t ->
            if (t == Token.LineBreak) lines += mutableListOf<Token>() else lines.last() += t
            lines
        }.filter { it.isNotEmpty() }

@Composable
fun DialogueInput(
    gaps: GapState,
    modifier: Modifier = Modifier,
) {
    val lines = dialogueLines(tokenize(gaps.exercise.text))
    val hintLine = lines.indexOfFirst { line -> line.any { it is Token.Gap } }
    val style = T.question.copy(fontSize = 19.sp, lineHeight = 34.sp)
    Column(modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEachIndexed { i, line ->
            val look = bubbleLook(student = i % 2 == 1)
            Box(Modifier.fillMaxWidth(), contentAlignment = look.alignment) {
                GapFlow(
                    tokens = line,
                    gaps = gaps,
                    style = style,
                    hint = gaps.exercise.hint.takeIf { i == hintLine },
                    modifier =
                        Modifier
                            .widthIn(max = 300.dp)
                            .clip(look.shape)
                            .background(look.background)
                            .then(look.border)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
