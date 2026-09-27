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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import dev.eftrainer.core.audio.AudioPlayerState
import dev.eftrainer.core.speech.rememberSpeechRecognizer
import dev.eftrainer.core.speech.TextToSpeechState
import androidx.compose.runtime.mutableIntStateOf
import dev.eftrainer.data.content.Talk
import dev.eftrainer.domain.ai.ChatLine
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.exercise.aiErrorRes
import dev.eftrainer.ui.exercise.talk.TalkViewModel
import dev.eftrainer.ui.exercise.talk.TalkViewModel.Status
import dev.eftrainer.ui.explain.LinkText
import dev.eftrainer.ui.explain.SendButton

/**
 * Разговор с ИИ прямо на экране задания: ИИ играет роль из сценария, ученик отвечает в поле внизу.
 * «Finish» или потолок реплик → ИИ оценивает разговор → [onGrade] (дальше обычный FeedbackSheet).
 */
@Composable
fun TalkInput(
    exercise: Talk,
    attempt: Int,
    checked: Boolean,
    onGrade: (correct: Boolean, summary: String, transcript: List<String>) -> Unit,
    tts: TextToSpeechState,
) {
    val container = appContainer()
    val vm = viewModel(key = "talk-${exercise.id}-$attempt") { TalkViewModel(container.aiClient, exercise) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val ungraded = stringResource(R.string.talk_ungraded)

    LaunchedEffect(ui.status) {
        val s = ui.status
        if (s is Status.Graded && !checked) onGrade(s.grade.pass, s.grade.summary.ifEmpty { ungraded }, ui.studentLines)
    }
    // Низ разговора всегда на экране: новая реплика или открылась клавиатура — прокрутка к полю.
    val bottom = remember { BringIntoViewRequester() }
    LaunchedEffect(ui.lines.size, ui.status) { bottom.bringIntoView() }

    // Реплики ИИ читаются вслух, каждая один раз (поворот экрана не повторяет). Тап по реплике — ещё раз / стоп.
    // Settings → Voice: выключено — сами не читаются (динамик у реплики работает); громкость и темп — к каждой реплике.
    // Синтезатор общий на экран сессии (rememberVoice в SessionScreen).
    val readAloud by container.voice.readAloud.collectAsStateWithLifecycle()
    var voiced by rememberSaveable(attempt) { mutableIntStateOf(0) }
    LaunchedEffect(attempt, ui.lines.size) {
        val last = ui.lines.lastOrNull()
        if (readAloud && ui.lines.size > voiced && last != null && !last.student && !checked) tts.speak(last.text, lineId(ui.lines.lastIndex))
        voiced = ui.lines.size
    }

    // key: поле реплики не должно переехать в следующий разговор той же сессии.
    key(attempt) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(exercise.goal, style = T.question.copy(fontSize = 19.sp, lineHeight = 28.sp))
        exercise.hint?.let { Text(it, style = T.hint) }
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.lines.forEachIndexed { i, line ->
                val id = lineId(i)
                val speaking = tts.speaking == id
                TalkBubble(
                    line,
                    speaking = speaking,
                    onTap = if (line.student || !tts.available) null else { { if (speaking) tts.stop() else tts.speak(line.text, id) } },
                )
            }
            if (ui.status == Status.Waiting) TalkBubble(ChatLine(false, "…"))
        }
        Column(Modifier.bringIntoViewRequester(bottom), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val small = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp)
            when (val s = ui.status) {
                Status.Grading -> Text(stringResource(R.string.talk_checking), style = small, modifier = Modifier.padding(vertical = 4.dp))
                is Status.Failed -> Text(
                    stringResource(aiErrorRes(s.reason)),
                    style = small.copy(color = C.Bad),
                    modifier = Modifier.clickable(onClick = vm::retry).padding(vertical = 4.dp),
                )
                else -> Unit
            }
            if (!checked && (ui.status == Status.Talking || ui.status == Status.Waiting)) {
                TalkField(enabled = ui.status == Status.Talking, onSend = vm::send, onMic = tts::stop)
                LinkText(
                    stringResource(R.string.talk_finish),
                    onClick = vm::finish,
                    enabled = ui.status == Status.Talking && ui.studentLines.isNotEmpty(),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    } }
}

private fun lineId(index: Int) = "talk-line-$index"

/**
 * Реплика: ИИ — слева бирюзовая плашка (как облачка English File), ученик — справа белая.
 * У реплики ИИ с озвучкой — маленький динамик; тап — прочитать ещё раз / замолчать.
 */
@Composable
private fun TalkBubble(line: ChatLine, speaking: Boolean = false, onTap: (() -> Unit)? = null) {
    val shape = if (line.student) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    val label = stringResource(if (speaking) R.string.talk_stop_reading else R.string.talk_read_aloud)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (line.student) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (line.student) C.Field else EF.Panel)
                .then(if (line.student) Modifier.border(1.dp, C.Line, shape) else Modifier)
                .then(if (onTap != null) Modifier.clickable(onClickLabel = label, onClick = onTap) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(line.text, style = T.rowEn.copy(fontSize = 17.sp, lineHeight = 25.sp), modifier = Modifier.weight(1f, fill = false))
            if (onTap != null) Icon(
                SpeakerIcon,
                contentDescription = label,
                modifier = Modifier.padding(top = 3.dp).size(16.dp),
                tint = if (speaking) C.Accent else C.Muted,
            )
        }
    }
}

/** Поле реплики. Без автозамены, как везде: ошибки ученика должны дойти до оценки. */
@Composable
private fun TalkField(enabled: Boolean, onSend: (String) -> Unit, onMic: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    // Голосом: распознанное попадает в поле — можно поправить и отправить.
    val speech = rememberSpeechRecognizer { hypotheses -> hypotheses.firstOrNull()?.let { text = it } }
    val send = {
        if (enabled && text.isNotBlank()) {
            onSend(text.trim())
            text = ""
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(shape).background(C.Field).border(1.5.dp, C.FieldBorder, shape).padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = T.field.copy(fontSize = 18.sp, lineHeight = 26.sp),
            cursorBrush = SolidColor(C.Accent),
            keyboardOptions = keyboard(ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) Text(
                    if (speech.listening) speech.partial.ifEmpty { "…" } else stringResource(R.string.talk_placeholder),
                    style = T.field.copy(fontSize = 18.sp, color = C.Muted),
                )
                inner()
            },
        )
        if (text.isBlank() && speech.available) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (speech.listening) C.Bad else if (enabled) C.Accent else C.Disabled)
                    .clickable(enabled = enabled) {
                        if (speech.listening) speech.stop() else { onMic(); AudioPlayerState.pauseAll(); speech.start() }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(MicIcon, stringResource(R.string.talk_mic), Modifier.size(20.dp), tint = C.OnAccent) }
        } else {
            SendButton(enabled = enabled && text.isNotBlank(), onClick = send)
        }
    }
}
