package dev.eftrainer.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.speech.TextToSpeechState
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.IconButton24
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.core.ui.ProgressBar
import dev.eftrainer.core.ui.SectionTag
import dev.eftrainer.core.ui.color
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Write
import dev.eftrainer.data.content.needsAi
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.exercise.inputs.AudioBar
import dev.eftrainer.ui.exercise.inputs.BlankInput
import dev.eftrainer.ui.exercise.inputs.RewriteInput
import dev.eftrainer.ui.exercise.inputs.SpeakInput
import dev.eftrainer.ui.exercise.inputs.TalkInput
import dev.eftrainer.ui.exercise.inputs.VideoBar
import dev.eftrainer.ui.exercise.inputs.WriteInput
import dev.eftrainer.ui.exercise.inputs.rememberVoice
import dev.eftrainer.ui.explain.ExplainHost
import dev.eftrainer.ui.explain.Selectable
import dev.eftrainer.ui.explain.explainContext

@Composable
fun SessionScreen(
    sectionId: String?,
    onClose: () -> Unit,
    vm: SessionViewModel = sessionViewModel(sectionId),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)

    when (val s = ui) {
        SessionUi.Loading -> {}

        SessionUi.Empty -> {
            LaunchedEffect(Unit) { currentOnClose() }
        }

        is SessionUi.Question -> {
            val aiOn by vm.aiOn.collectAsStateWithLifecycle()
            val feedback = s.feedback?.takeIf { !s.exercise.needsAi }
            val context =
                remember(
                    s.exercise.id,
                    feedback,
                ) { explainContext(s.exercise.id, feedback?.answer, feedback?.expected) }
            ExplainHost(context) {
                QuestionScreen(
                    s,
                    aiOn,
                    onCheck = vm::check,
                    onGrade = vm::grade,
                    onNext = vm::next,
                    onExplain = vm::explain,
                    onClose = onClose,
                )
            }
        }

        is SessionUi.Done -> {
            DoneScreen(s, onClose)
        }

        is SessionUi.ChapterDone -> {
            ChapterDoneScreen(s, onClose)
        }
    }
}

@Composable
private fun sessionViewModel(sectionId: String?): SessionViewModel =
    appContainer().let { container ->
        viewModel(key = "session-$sectionId") { SessionViewModel(container, sectionId) }
    }

private fun fieldCount(exercise: Exercise): Int =
    when (exercise) {
        is Rewrite, is Speak, is Write -> 1
        is Blank -> exercise.gaps.size
        is Talk -> 0
    }

@Stable
private class AnswerFields(
    val values: SnapshotStateList<TextFieldValue>,
    val focus: List<FocusRequester>,
) {
    val filled: Boolean get() = values.all { it.text.isNotBlank() }

    fun texts(): List<String> = values.map { it.text }
}

private fun initialValues(exercise: Exercise): SnapshotStateList<TextFieldValue> =
    mutableStateListOf<TextFieldValue>().apply {
        val prefill = (exercise as? Rewrite)?.prefill
        if (prefill != null) {
            add(TextFieldValue(prefill, TextRange(prefill.length)))
        } else {
            repeat(fieldCount(exercise)) { add(TextFieldValue()) }
        }
    }

@Composable
private fun rememberAnswerFields(
    exercise: Exercise,
    attempt: Int,
): AnswerFields =
    remember(attempt) {
        AnswerFields(initialValues(exercise), List(fieldCount(exercise)) { FocusRequester() })
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
    val answer = rememberAnswerFields(e, q.attempt)
    LaunchedEffect(q.attempt) { answer.focus.firstOrNull()?.requestFocus() }
    val tts = rememberVoice()
    LaunchedEffect(e.id, q.attempt) { tts.stop() }

    val feedback = q.feedback
    val filled = answer.filled
    val action = {
        if (feedback != null) {
            onNext()
        } else if (filled) {
            onCheck(answer.texts())
        }
    }

    Column(Modifier.fillMaxSize()) {
        QuestionTopBar(q, onClose)

        Selectable(Modifier.weight(1f)) {
            Column(
                Modifier
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TaskHeader(q)
                ExerciseInput(
                    q = q,
                    answer = answer,
                    tts = tts,
                    onAction = action,
                    onGrade = onGrade,
                )
            }
        }

        if (feedback == null) {
            if (e !is Talk) CheckBar(enabled = filled && q.aiCheck != AiCheck.Checking, onClick = action)
        } else {
            FeedbackSheet(
                feedback,
                q.explanation,
                aiOn,
                aiGraded = e.needsAi,
                onNext = onNext,
                onExplain = onExplain,
                tts = tts,
                sayId = "answer-${e.id}-${q.attempt}",
            )
        }
    }
}

@Composable
private fun CheckBar(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    BottomBar {
        PrimaryButton(stringResource(R.string.check), onClick = onClick, enabled = enabled)
    }
}

@Composable
private fun QuestionTopBar(
    q: SessionUi.Question,
    onClose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton24(Icons.Close, stringResource(R.string.close), onClose)
        ProgressBar(q.done.toFloat() / q.total, Modifier.weight(1f))
        Text(stringResource(R.string.n_of_m, q.done, q.total), style = T.label.copy(fontSize = 15.sp))
    }
}

@Composable
private fun TaskHeader(q: SessionUi.Question) {
    val e = q.exercise
    val kind = remember(e.id) { Chapters.locate(e.id).section.kind }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTag(kind)
            Text(
                q.task.instruction,
                style =
                    T.label.copy(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kind.color,
                    ),
                modifier = Modifier.weight(1f),
            )
        }
        q.task.video?.let { VideoBar(it, clip = e.clip) }
        q.task.audio?.let { AudioBar(it, clip = e.clip) }
        q.task.context?.let {
            Text(
                it,
                style = T.rowEn.copy(fontSize = 16.sp, lineHeight = 24.sp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Field)
                        .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                        .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ExerciseInput(
    q: SessionUi.Question,
    answer: AnswerFields,
    tts: TextToSpeechState,
    onAction: () -> Unit,
    onGrade: (correct: Boolean, summary: String, transcript: List<String>) -> Unit,
) {
    val feedback = q.feedback
    val checked = feedback != null
    val fields = answer.values
    val focus = answer.focus
    when (val e = q.exercise) {
        is Rewrite -> {
            RewriteInput(
                exercise = e,
                value = fields[0],
                onChange = { fields[0] = it },
                checked = checked,
                correct = feedback?.correct == true,
                focus = focus[0],
                onAction = onAction,
            )
        }

        is Blank -> {
            BlankInput(
                exercise = e,
                values = fields,
                onChange = { i, v -> fields[i] = v },
                checked = checked,
                wrongGaps = feedback?.wrongGaps.orEmpty(),
                focus = focus,
                onAction = onAction,
            )
        }

        is Speak -> {
            SpeakInput(
                exercise = e,
                attempt = q.attempt,
                value = fields[0],
                onChange = { fields[0] = it },
                checked = checked,
                correct = feedback?.correct == true,
                focus = focus[0],
                onAction = onAction,
                tts = tts,
            )
        }

        is Talk -> {
            TalkInput(e, q.attempt, checked, onGrade, tts)
        }

        is Write -> {
            WriteInput(
                exercise = e,
                value = fields[0],
                onChange = { fields[0] = it },
                checked = checked,
                focus = focus[0],
                aiCheck = q.aiCheck,
                onRetry = onAction,
            )
        }
    }
}
