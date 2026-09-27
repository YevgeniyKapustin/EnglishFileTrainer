package dev.eftrainer.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BandInk
import dev.eftrainer.core.ui.Bubble
import dev.eftrainer.core.ui.SectionTag
import dev.eftrainer.core.ui.WedgeBand
import dev.eftrainer.core.ui.bubbleText
import dev.eftrainer.core.ui.color
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.IconButton24
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Write
import dev.eftrainer.data.content.needsAi
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.explain.ExplainHost
import dev.eftrainer.ui.explain.Selectable
import dev.eftrainer.ui.explain.explainContext
import dev.eftrainer.ui.exercise.inputs.BlankInput
import dev.eftrainer.ui.exercise.inputs.AudioBar
import dev.eftrainer.ui.exercise.inputs.VideoBar
import dev.eftrainer.ui.exercise.inputs.RewriteInput
import dev.eftrainer.ui.exercise.inputs.SpeakInput
import dev.eftrainer.ui.exercise.inputs.TalkInput
import dev.eftrainer.ui.exercise.inputs.WriteInput
import dev.eftrainer.ui.exercise.inputs.rememberVoice

/** sectionId = null — Review. [onClose] — назад на текущую главу. */
@Composable
fun SessionScreen(sectionId: String?, onClose: () -> Unit) {
    val container = appContainer()
    val vm = viewModel(key = "session-$sectionId") { SessionViewModel(container, sectionId) }
    val ui by vm.state.collectAsStateWithLifecycle()

    when (val s = ui) {
        SessionUi.Loading -> Unit
        SessionUi.Empty -> LaunchedEffect(Unit) { onClose() }
        is SessionUi.Question -> {
            val aiOn by vm.aiOn.collectAsStateWithLifecycle()
            // «Explain» на выделенный текст: в запрос уходит это упражнение (и ответ, если уже проверено).
            val feedback = s.feedback?.takeIf { !s.exercise.needsAi }
            val context = remember(s.exercise.id, feedback) { explainContext(s.exercise.id, feedback?.answer, feedback?.expected) }
            ExplainHost(context) {
                QuestionScreen(s, aiOn, onCheck = vm::check, onGrade = vm::grade, onNext = vm::next, onExplain = vm::explain, onClose = onClose)
            }
        }
        is SessionUi.Done -> DoneScreen(s, onClose)
        is SessionUi.ChapterDone -> ChapterDoneScreen(s, onClose)
    }
}

@Composable
private fun QuestionScreen(
    q: SessionUi.Question,
    aiOn: Boolean,
    onCheck: (List<String>) -> Unit,
    onGrade: (correct: Boolean, summary: String, transcript: List<String>) -> Unit,
    onNext: () -> Unit,
    onExplain: (question: String?) -> Unit,
    onClose: () -> Unit,
) {
    val e = q.exercise
    val count = when (e) {
        is Rewrite, is Speak, is Write -> 1
        is Blank -> e.gaps.size
        is Talk -> 0
    }
    // Поля живут одну попытку: новое упражнение (или повтор после ошибки) — пустые поля.
    val fields = remember(q.attempt) {
        mutableStateListOf<TextFieldValue>().apply {
            val prefill = (e as? Rewrite)?.prefill
            if (prefill != null) add(TextFieldValue(prefill, TextRange(prefill.length)))
            else repeat(count) { add(TextFieldValue()) }
        }
    }
    val focus = remember(q.attempt) { List(count) { FocusRequester() } }
    LaunchedEffect(q.attempt) { focus.firstOrNull()?.requestFocus() }
    // Один синтезатор на экран сессии: «Listen» в Speak, динамик в ответе, реплики разговора.
    // Новое упражнение (или повтор) — замолкает; уход с экрана — освобождается.
    val tts = rememberVoice()
    LaunchedEffect(e.id, q.attempt) { tts.stop() }

    val feedback = q.feedback
    val filled = fields.all { it.text.isNotBlank() }
    val action = {
        if (feedback != null) onNext() else if (filled) onCheck(fields.map { it.text })
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton24(Icons.Close, stringResource(R.string.close), onClose)
            ProgressBar(q.done.toFloat() / q.total, Modifier.weight(1f))
            Text(stringResource(R.string.n_of_m, q.done, q.total), style = T.label.copy(fontSize = 15.sp))
        }

        Selectable(Modifier.weight(1f)) { Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val kind = remember(e.id) { Chapters.locate(e.id).section.kind }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTag(kind)
                Text(
                    q.task.instruction,
                    style = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = kind.color),
                    modifier = Modifier.weight(1f),
                )
            }
            q.task.video?.let { VideoBar(it, e.clip) }
            q.task.audio?.let { AudioBar(it, e.clip) }
            q.task.context?.let {
                Text(
                    it,
                    style = T.rowEn.copy(fontSize = 16.sp, lineHeight = 24.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Field)
                        .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                )
            }
            when (e) {
                is Rewrite -> RewriteInput(
                    exercise = e,
                    value = fields[0],
                    onChange = { fields[0] = it },
                    checked = feedback != null,
                    correct = feedback?.correct == true,
                    focus = focus[0],
                    onAction = action,
                )
                is Blank -> BlankInput(
                    exercise = e,
                    values = fields,
                    onChange = { i, v -> fields[i] = v },
                    checked = feedback != null,
                    wrongGaps = feedback?.wrongGaps.orEmpty(),
                    focus = focus,
                    onAction = action,
                )
                is Speak -> SpeakInput(
                    exercise = e,
                    attempt = q.attempt,
                    value = fields[0],
                    onChange = { fields[0] = it },
                    checked = feedback != null,
                    correct = feedback?.correct == true,
                    focus = focus[0],
                    onAction = action,
                    tts = tts,
                )
                is Talk -> TalkInput(exercise = e, attempt = q.attempt, checked = feedback != null, onGrade = onGrade, tts = tts)
                is Write -> WriteInput(
                    exercise = e,
                    value = fields[0],
                    onChange = { fields[0] = it },
                    checked = feedback != null,
                    focus = focus[0],
                    aiCheck = q.aiCheck,
                    onRetry = action,
                )
            }
        } }

        if (feedback == null) {
            // Разговор сам решает, когда закончен (TalkInput → onGrade).
            // Письменный ответ на проверке у ИИ — кнопка ждёт.
            if (e !is Talk) BottomBar { PrimaryButton(stringResource(R.string.check), onClick = action, enabled = filled && q.aiCheck != AiCheck.Checking) }
        } else {
            FeedbackSheet(feedback, q.explanation, aiOn, aiGraded = e.needsAi, onNext = onNext, onExplain = onExplain, tts = tts, sayId = "answer-${e.id}-${q.attempt}")
        }
    }
}

@Composable
private fun DoneScreen(d: SessionUi.Done, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            WedgeBand(128.dp) {
                Text(
                    stringResource(R.string.done),
                    style = TextStyle(fontFamily = Golos, fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, color = BandInk),
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 16.dp),
                )
                Bubble(100.dp, EF.Lime, Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 16.dp)) {
                    Text(stringResource(R.string.ef_score, d.firstTry, d.total), style = bubbleText.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.ef_right_first_time), style = bubbleText)
                }
            }
            Column(Modifier.padding(horizontal = 24.dp)) {
                if (d.chapterId != null) {
                    Text(
                        stringResource(R.string.chapter_n, d.chapterId),
                        style = T.label.copy(color = EF.Title, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 32.dp),
                    )
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProgressBar(d.after / 100f, Modifier.weight(1f))
                        Text(
                            stringResource(R.string.percent_change, d.before, d.after),
                            style = T.body.copy(fontSize = 15.sp),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
                if (d.mistakes.isNotEmpty()) {
                    Text(
                        stringResource(R.string.mistakes_back_tomorrow),
                        style = T.label.copy(color = C.Bad, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 32.dp),
                    )
                    d.mistakes.forEachIndexed { i, m ->
                        ListRow(onClick = null, divider = i != d.mistakes.lastIndex) { Text(m, style = T.rowEn.copy(fontSize = 18.sp, lineHeight = 27.sp)) }
                    }
                }
            }
        }
        BottomBar { PrimaryButton(stringResource(R.string.close), onClick = onClose) }
    }
}

@Composable
private fun ChapterDoneScreen(c: SessionUi.ChapterDone, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            WedgeBand(300.dp) {
                Text(
                    c.chapter.id,
                    style = TextStyle(fontFamily = Golos, fontSize = 96.sp, lineHeight = 96.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp, color = BandInk),
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 28.dp),
                )
                c.next?.let { next ->
                    Bubble(116.dp, EF.Lime, Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 30.dp)) {
                        Text(stringResource(R.string.ef_next), style = bubbleText)
                        Text(next.id, style = bubbleText.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold))
                        Text(next.title, style = bubbleText, maxLines = 2)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.chapter_complete, c.chapter.id),
                    style = T.title.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = EF.Title),
                )
                Text(
                    if (c.next != null) stringResource(R.string.next_is_open, c.next.id, c.chapter.id)
                    else stringResource(R.string.book_complete_note, c.chapter.id),
                    style = T.body.copy(color = C.Muted),
                )
            }
        }
        BottomBar {
            PrimaryButton(
                if (c.next != null) stringResource(R.string.start_chapter, c.next.id) else stringResource(R.string.close),
                onClick = onClose,
            )
        }
    }
}
