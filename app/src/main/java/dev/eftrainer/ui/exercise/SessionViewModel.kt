package dev.eftrainer.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Speak
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Task
import dev.eftrainer.data.content.Write
import dev.eftrainer.data.content.exercises
import dev.eftrainer.domain.ai.AiChat
import dev.eftrainer.domain.ai.ExplainPrompt
import dev.eftrainer.domain.ai.RuleScope
import dev.eftrainer.domain.ai.WritePrompt
import dev.eftrainer.domain.ai.reply
import dev.eftrainer.domain.check.Checker
import dev.eftrainer.domain.check.expected
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.DayInputs
import dev.eftrainer.domain.learning.DayStep
import dev.eftrainer.domain.learning.GenerationPlan
import dev.eftrainer.domain.learning.Mastery
import dev.eftrainer.domain.learning.Stats
import dev.eftrainer.domain.learning.TrainingQueue
import dev.eftrainer.ui.toPercent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data class Feedback(
    val correct: Boolean,
    val expected: String,
    val wrongGaps: Set<Int>,
    val explain: String?,
    val answer: String,
    val corrections: List<WritePrompt.Correction> = emptyList(),
)

sealed interface AiCheck {
    data object Checking : AiCheck

    data class Failed(
        val reason: AiException.Reason,
    ) : AiCheck
}

typealias Explanation = AiChat

sealed interface SessionUi {
    data object Loading : SessionUi

    data object Empty : SessionUi

    data class Question(
        val exercise: Exercise,
        val task: Task,
        val attempt: Int,
        val done: Int,
        val total: Int,
        val feedback: Feedback?,
        val explanation: Explanation? = null,
        val aiCheck: AiCheck? = null,
    ) : SessionUi

    data class Done(
        val firstTry: Int,
        val total: Int,
        val chapterId: String?,
        val before: Int,
        val after: Int,
        val mistakes: List<String>,
        val streak: Int,
    ) : SessionUi

    data class ChapterDone(
        val chapter: Chapter,
        val next: Chapter?,
    ) : SessionUi
}

class SessionViewModel(
    private val container: AppContainer,
    private val sectionId: String?,
    private val again: Boolean = false,
    private val taskId: String? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<SessionUi>(SessionUi.Loading)
    val state: StateFlow<SessionUi> = _state.asStateFlow()

    val aiOn: StateFlow<Boolean> =
        container.aiOn
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val queue = ArrayDeque<Exercise>()
    private var total = 0
    private var done = 0
    private var attempt = 0
    private val answered = mutableSetOf<String>()
    private val wrong = linkedSetOf<Exercise>()
    private val writes = mutableListOf<Job>()
    private var completedChapter: Chapter? = null
    private var chapter: Chapter? = null
    private var before = 0
    private var arrivals = 0
    private var reviewing = false

    init {
        val cached = container.day.latest
        if (cached != null) {
            start(cached)
        } else {
            viewModelScope.launch { start(container.day.inputs.first()) }
        }
    }

    private fun start(inputs: DayInputs) {
        val progress = inputs.review.progress
        val ai = inputs.review.ai
        val day = if (sectionId == null) container.day.plan(inputs) else null
        reviewing = day?.step == DayStep.Review
        if (reviewing) acceptFresh()
        if (ai) container.generation.refillInBackground()
        val exercises =
            when {
                sectionId == null -> day?.exercises.orEmpty()
                taskId != null -> TrainingQueue.task(Chapters.section(sectionId), taskId, ai)
                again -> TrainingQueue.again(Chapters.section(sectionId), ai)
                else -> TrainingQueue.firstPass(Chapters.section(sectionId), progress, ai)
            }
        chapter =
            if (sectionId == null) {
                day?.step?.chapter ?: ChapterCompletion.current(Chapters.all, inputs.review.completed)
            } else {
                Chapters.chapterOfSection(sectionId)
            }
        before = chapter?.let { Mastery.share(it.exercises, progress) }?.toPercent() ?: 0

        queue.addAll(exercises)
        total = exercises.size
        if (queue.isEmpty()) _state.value = SessionUi.Empty else show()
    }

    private fun acceptFresh() {
        viewModelScope.launch {
            container.generation.fresh.collect { items ->
                val empty = _state.value == SessionUi.Empty
                if (_state.value !is SessionUi.Question && !empty) return@collect
                val added = TrainingQueue.arrivals(items, answered + queue.map { it.id }, arrivals)
                if (added.isEmpty()) return@collect
                queue.addAll(added)
                arrivals += added.size
                total += added.size
                val q = _state.value
                if (empty) {
                    show()
                } else if (q is SessionUi.Question) {
                    _state.value = q.copy(total = total)
                }
            }
        }
    }

    fun check(inputs: List<String>) {
        val q = _state.value as? SessionUi.Question ?: return
        if (q.feedback != null) return
        val e = q.exercise
        val result =
            when (e) {
                is Rewrite -> Checker.check(e, inputs.single())
                is Blank -> Checker.check(e, inputs)
                is Speak -> Checker.check(e, inputs.single())
                is Order -> Checker.check(e, inputs)
                is Talk -> return
                is Write -> return gradeWriting(q, e, inputs.single())
            }
        record(q, result.correct, result.expected, result.wrongGaps, ExplainPrompt.answerText(e, inputs))
    }

    fun grade(
        correct: Boolean,
        summary: String,
        transcript: List<String>,
    ) {
        val q = _state.value as? SessionUi.Question ?: return
        if (q.feedback != null) return
        record(q, correct, summary, emptySet(), ExplainPrompt.answerText(q.exercise, transcript))
    }

    private fun gradeWriting(
        q: SessionUi.Question,
        e: Write,
        text: String,
    ) {
        if (q.aiCheck == AiCheck.Checking || text.isBlank()) return
        _state.value = q.copy(aiCheck = AiCheck.Checking)
        viewModelScope.launch {
            val loc = Chapters.locate(e.id)
            val prompt =
                WritePrompt.grading(
                    e,
                    loc.task.instruction,
                    ExplainPrompt.rules(RuleScope.forChapter(Chapters.all, loc.chapter, loc.section)),
                    text,
                )
            val client = container.aiClient
            val grade =
                try {
                    WritePrompt.parseGrade(client.chat(prompt))
                        ?: WritePrompt.parseGrade(client.chat(prompt))
                        ?: WritePrompt.Grade(pass = false, summary = "", corrections = emptyList())
                } catch (ex: AiException) {
                    val now = _state.value as? SessionUi.Question
                    if (now?.attempt == q.attempt) _state.value = now.copy(aiCheck = AiCheck.Failed(ex.reason))
                    return@launch
                }
            val now = _state.value as? SessionUi.Question ?: return@launch
            if (now.attempt != q.attempt || now.feedback != null) return@launch
            record(now.copy(aiCheck = null), grade.pass, grade.summary, emptySet(), text.trim(), grade.corrections)
        }
    }

    private fun record(
        q: SessionUi.Question,
        correct: Boolean,
        expected: String,
        wrongGaps: Set<Int>,
        answer: String,
        corrections: List<WritePrompt.Correction> = emptyList(),
    ) {
        val e = q.exercise
        val result = Feedback(correct, expected, wrongGaps, e.explain, answer, corrections)
        if (answered.add(e.id)) {
            writes +=
                viewModelScope.launch {
                    container.learning.answer(e, result.correct)?.let { completedChapter = it }
                    if (aiOn.value && GenerationPlan.eligible(Chapters.locate(e.id).section)) {
                        container.generation.refillInBackground()
                    }
                }
        }
        if (result.correct) done++ else wrong += e
        _state.value = q.copy(done = done, feedback = result)
    }

    fun explain(question: String? = null) {
        val q = _state.value as? SessionUi.Question ?: return
        val f = q.feedback ?: return
        val current = q.explanation
        if (f.correct || current?.loading == true) return
        val chat =
            when {
                current == null -> {
                    val loc = Chapters.locate(q.exercise.id)
                    AiChat(
                        ExplainPrompt.build(
                            loc.chapter,
                            loc.section,
                            loc.task,
                            q.exercise,
                            f.answer,
                            f.expected,
                            RuleScope.forExercise(Chapters.all, loc.chapter, loc.section),
                        ),
                    ).start()
                }

                question != null -> {
                    if (question.isBlank() || !current.answered) return else current.ask(question)
                }

                current.error != null -> {
                    current.start()
                }

                else -> {
                    return
                }
            }
        _state.value = q.copy(explanation = chat)
        viewModelScope.launch {
            val result = container.aiClient.reply(chat)
            val now = _state.value as? SessionUi.Question ?: return@launch
            if (now.attempt == q.attempt) _state.value = now.copy(explanation = result)
        }
    }

    fun next() {
        val q = _state.value as? SessionUi.Question ?: return
        val f = q.feedback ?: return
        queue.removeFirst()
        if (!f.correct) queue.addLast(q.exercise)
        if (queue.isEmpty()) finish() else show()
    }

    private fun show() {
        val e = queue.first()
        _state.value = SessionUi.Question(e, Chapters.locate(e.id).task, ++attempt, done, total, null)
    }

    private fun finish() {
        viewModelScope.launch {
            writes.joinAll()
            if (reviewing) container.day.reviewFinished()
            val done = completedChapter
            if (done != null) {
                val next = ChapterCompletion.current(Chapters.all, container.chapters.getCompleted())
                _state.value = SessionUi.ChapterDone(done, next)
                return@launch
            }
            val progress = container.progress.getAll()
            _state.value =
                SessionUi.Done(
                    firstTry = total - wrong.size,
                    total = total,
                    chapterId = chapter?.id,
                    before = before,
                    after = chapter?.let { Mastery.share(it.exercises, progress) }?.toPercent() ?: 0,
                    mistakes = wrong.map { it.expected() },
                    streak = Stats.streak(container.answers.days.first(), container.clock.today()).current,
                )
        }
    }

    private fun Exercise.expected(): String =
        when (this) {
            is Rewrite -> answers.first()
            is Blank -> Checker.check(this, gaps.map { it.first() }).expected
            is Speak -> text
            is Order -> expected()
            is Talk -> goal
            is Write -> prompt
        }
}
