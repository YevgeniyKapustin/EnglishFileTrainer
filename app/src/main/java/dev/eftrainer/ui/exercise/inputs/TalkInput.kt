package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.linkClickable
import dev.eftrainer.data.content.Talk
import dev.eftrainer.domain.ai.ChatLine
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.exercise.aiErrorRes
import dev.eftrainer.ui.exercise.talk.TalkViewModel
import dev.eftrainer.ui.exercise.talk.TalkViewModel.Status
import dev.eftrainer.ui.explain.LinkText

@Composable
fun TalkInput(
    exercise: Talk,
    attempt: Int,
    checked: Boolean,
    onGrade: (correct: Boolean, summary: String, transcript: List<String>) -> Unit,
    tts: TextToSpeechState,
    modifier: Modifier = Modifier,
    vm: TalkViewModel = talkViewModel(exercise, attempt),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val ungraded = stringResource(R.string.talk_ungraded)
    val currentOnGrade by rememberUpdatedState(onGrade)

    LaunchedEffect(ui.status) {
        val s = ui.status
        if (s is Status.Graded && !checked) {
            currentOnGrade(s.grade.pass, s.grade.summary.ifEmpty { ungraded }, ui.studentLines)
        }
    }
    val bottom = remember { BringIntoViewRequester() }
    LaunchedEffect(ui.lines.size, ui.status) { bottom.bringIntoView() }
    ReadAloudEffect(attempt, ui.lines, checked, tts)

    key(attempt) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exercise.pictures.isNotEmpty()) TalkPictures(exercise.pictures)
            Text(exercise.goal, style = T.question.copy(fontSize = 19.sp, lineHeight = 28.sp))
            if (exercise.steps.isNotEmpty()) {
                val active = !checked && (ui.status == Status.Talking || ui.status == Status.Waiting)
                TalkSteps(exercise.steps, ui.done, active)
            }
            exercise.hint?.let { Text(it, style = T.hint) }
            TalkTranscript(ui, tts)
            TalkControls(
                ui = ui,
                checked = checked,
                onRetry = vm::retry,
                onSend = vm::send,
                onFinish = vm::finish,
                onMic = tts::stop,
                modifier = Modifier.bringIntoViewRequester(bottom),
            )
        }
    }
}

@Composable
private fun talkViewModel(
    exercise: Talk,
    attempt: Int,
): TalkViewModel =
    appContainer().let { container ->
        viewModel(key = "talk-${exercise.id}-$attempt") { TalkViewModel(container.aiClient, exercise) }
    }

@Composable
private fun ReadAloudEffect(
    attempt: Int,
    lines: List<ChatLine>,
    checked: Boolean,
    tts: TextToSpeechState,
) {
    val readAloud by appContainer().voice.readAloud.collectAsStateWithLifecycle()
    var voiced by rememberSaveable(attempt) { mutableIntStateOf(0) }
    LaunchedEffect(attempt, lines.size) {
        val tutorLine = lines.lastOrNull()?.takeIf { !it.student }
        val shouldRead = readAloud && !checked && lines.size > voiced
        if (shouldRead && tutorLine != null) tts.speak(tutorLine.text, lineId(lines.lastIndex))
        voiced = lines.size
    }
}

private fun readAction(
    line: ChatLine,
    id: String,
    tts: TextToSpeechState,
): (() -> Unit)? {
    val speaking = tts.speaking == id
    return { if (speaking) tts.stop() else tts.speak(line.text, id) }
        .takeIf { !line.student && tts.available }
}

@Composable
private fun TalkTranscript(
    ui: TalkViewModel.Ui,
    tts: TextToSpeechState,
) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.lines.forEachIndexed { i, line ->
            val id = lineId(i)
            TalkBubble(line, speaking = tts.speaking == id, onTap = readAction(line, id, tts))
        }
        if (ui.status == Status.Waiting) TalkBubble(ChatLine(false, "…"))
    }
}

@Composable
private fun TalkControls(
    ui: TalkViewModel.Ui,
    checked: Boolean,
    onRetry: () -> Unit,
    onSend: (String) -> Unit,
    onFinish: () -> Unit,
    onMic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val talking = ui.status == Status.Talking
    val inConversation = talking || ui.status == Status.Waiting
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val small = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp)
        when (val s = ui.status) {
            Status.Grading -> {
                Text(
                    stringResource(R.string.talk_checking),
                    style = small,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            is Status.Failed -> {
                Text(
                    stringResource(aiErrorRes(s.reason)),
                    style = small.copy(color = C.Bad),
                    modifier = Modifier.linkClickable(onClick = onRetry).padding(vertical = 4.dp),
                )
            }

            else -> {}
        }
        if (!checked && inConversation) {
            TalkField(enabled = talking, onSend = onSend, onMic = onMic)
            LinkText(
                stringResource(R.string.talk_finish),
                onClick = onFinish,
                enabled = talking && ui.studentLines.isNotEmpty(),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

private fun lineId(index: Int) = "talk-line-$index"

@Composable
private fun TalkBubble(
    line: ChatLine,
    speaking: Boolean = false,
    onTap: (() -> Unit)? = null,
) {
    val look = bubbleLook(line.student)
    val label = stringResource(if (speaking) R.string.talk_stop_reading else R.string.talk_read_aloud)
    val tap = if (onTap != null) Modifier.clickable(onClickLabel = label, onClick = onTap) else Modifier
    val iconTint = if (speaking) C.Accent else C.Muted
    Box(Modifier.fillMaxWidth(), contentAlignment = look.alignment) {
        Row(
            Modifier
                .widthIn(max = 300.dp)
                .clip(look.shape)
                .background(look.background)
                .then(look.border)
                .then(tap)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                line.text,
                style = T.rowEn.copy(fontSize = 17.sp, lineHeight = 25.sp),
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onTap != null) {
                Icon(
                    SpeakerIcon,
                    contentDescription = label,
                    modifier = Modifier.padding(top = 3.dp).size(16.dp),
                    tint = iconTint,
                )
            }
        }
    }
}

internal class BubbleLook(
    val shape: Shape,
    val alignment: Alignment,
    val background: Color,
    val border: Modifier,
)

internal fun bubbleLook(student: Boolean): BubbleLook =
    if (student) {
        val shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
        BubbleLook(shape, Alignment.CenterEnd, C.Field, Modifier.border(1.dp, C.Line, shape))
    } else {
        BubbleLook(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp), Alignment.CenterStart, EF.Panel, Modifier)
    }
