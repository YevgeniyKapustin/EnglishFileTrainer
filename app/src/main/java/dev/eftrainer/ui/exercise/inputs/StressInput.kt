package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.sound.Cue
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.content.Stress
import dev.eftrainer.ui.appContainer

@Composable
fun StressInput(
    exercise: Stress,
    picks: List<Int>,
    onPick: (List<Int>) -> Unit,
    checked: Boolean,
    wrong: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val sounds = appContainer().sounds
    val look = StressLook(exercise, picks, checked, wrong)
    val tap = { i: Int ->
        if (!checked) {
            onPick(
                when {
                    exercise.syllables -> listOf(i)
                    i in picks -> picks - i
                    else -> (picks + i).sorted()
                },
            )
            sounds.play(Cue.TICK)
        }
    }
    if (exercise.syllables) {
        Syllables(exercise, look, tap, modifier)
    } else {
        Sentence(exercise, look, tap, modifier)
    }
}

private class StressLook(
    private val exercise: Stress,
    private val picks: List<Int>,
    private val checked: Boolean,
    private val wrong: Set<Int>,
) {
    fun color(i: Int): Color =
        when {
            !checked -> if (i in picks) C.Accent else C.Ink
            i in wrong -> C.Bad
            i in exercise.stressed || !exercise.syllables -> C.Ok
            else -> C.Ink
        }

    fun underlined(i: Int): Boolean = if (checked) i in exercise.stressed || i in wrong else i in picks
}

@Composable
private fun Syllables(
    exercise: Stress,
    look: StressLook,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        exercise.parts.forEachIndexed { i, syllable ->
            if (i > 0) Text("|", style = T.question.copy(fontSize = 30.sp, color = C.Chevron))
            Text(
                syllable,
                style =
                    T.question.copy(
                        fontSize = 34.sp,
                        lineHeight = 44.sp,
                        color = look.color(i),
                        textDecoration = if (look.underlined(i)) TextDecoration.Underline else null,
                    ),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTap(i) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Sentence(
    exercise: Stress,
    look: StressLook,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text =
        buildAnnotatedString {
            exercise.parts.forEachIndexed { i, part ->
                if (i in exercise.targets) {
                    val style =
                        SpanStyle(
                            color = look.color(i),
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (look.underlined(i)) TextDecoration.Underline else null,
                        )
                    withLink(LinkAnnotation.Clickable("t$i", TextLinkStyles(style)) { onTap(i) }) {
                        withStyle(style) { append(part) }
                    }
                } else {
                    append(part)
                }
            }
        }
    Text(text, style = T.question, modifier = modifier.fillMaxWidth())
}
