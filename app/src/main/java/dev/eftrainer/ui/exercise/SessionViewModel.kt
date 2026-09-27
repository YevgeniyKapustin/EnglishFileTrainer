package dev.eftrainer.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.AppContainer
import dev.eftrainer.core.sound.Cue
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
import dev.eftrainer.data.content.needsAi
import dev.eftrainer.domain.ai.AiChat
import dev.eftrainer.domain.ai.ExplainPrompt
import dev.eftrainer.domain.ai.PersonalQuestion
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
import dev.eftrainer.domain.learning.SessionTally
import dev.eftrainer.domain.learning.Stats
import dev.eftrainer.domain.learning.TrainingQueue
import dev.eftrainer.ui.toPercent
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
        val usedWords: Set<String> = emptySet(),
        val answerBack: AnswerBack? = null,
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
    private val sectionId: String? = null,
    private val taskId: String? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<SessionUi>(SessionUi.Loading)
    val state: StateFlow<SessionUi> = _state.asStateFlow()

    val aiOn: StateFlow<Boolean> =
        container.aiOn
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val answerBacks = AnswerBackChecks(_state, viewModelScope, container.aiClient)

    private var tally = SessionTally(emptyList())
    private var attempt = 0
    private val writes = mutableListOf<Deferred<Chapter?>>()
    private val run = SessionRun(container.taskRuns, taskId.takeIf { sectionId != null })
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
        val picked = if (sectionId != null && taskId != null) Chapters.section(sectionId) to taskId else null
        val day = if (picked == null) container.day.plan(inputs) else null
        reviewing = day?.step == DayStep.Review
        if (reviewing) acceptFresh()
        if (ai) container.generation.refillInBackground()
        val exercises =
            picked?.let { (section, task) -> TrainingQueue.task(section, task, ai) } ?: day?.exercises.orEmpty()
        chapter =
            picked?.let { (section, _) -> Chapters.chapterOfSection(section.id) }
                ?: day?.step?.chapter
                ?: ChapterCompletion.current(Chapters.all, inputs.review.completed)
        before = chapter?.let { Mastery.share(it.exercises, progress) }?.toPercent() ?: 0

        tally = run.tally(exercises)
        if (tally.finished) _state.value = SessionUi.Empty else show()
    }

    private fun acceptFresh() {
        viewModelScope.launch {
            container.generation.fresh.collect { items ->
                val empty = _state.value == SessionUi.Empty
                if (_state.value !is SessionUi.Question && !empty) return@collect
                val added = TrainingQueue.arrivals(items, tally.seenIds, arrivals)
                if (added.isEmpty()) return@collect
                tally.add(added)
                arrivals += added.size
                val q = _state.value
                if (empty) {
                    show()
                } else if (q is SessionUi.Question) {
                    _state.value = q.copy(total = tally.total)
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
        record(q, result.correct, result.expected, result.wrongGaps, ExplainPrompt.answerText(e, inputs), inputs)
    }

    fun grade(
        correct: Boolean,
        summary: String,
        transcript: List<String>,
    ) {
        val q = _state.value as? SessionUi.Question ?: return
        if (q.feedback != null) return
        record(q, correct, summary, emptySet(), ExplainPrompt.answerText(q.exercise, transcript), transcript)
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
            record(
                now.copy(aiCheck = null),
                grade.pass,
                grade.summary,
                emptySet(),
                text.trim(),
                listOf(text),
                grade.corrections,
            )
        }
    }

    private fun record(
        q: SessionUi.Question,
        correct: Boolean,
        expected: String,
        wrongGaps: Set<Int>,
        answer: String,
        inputs: List<String>,
        corrections: List<WritePrompt.Correction> = emptyList(),
    ) {
        val e = q.exercise
        val result = Feedback(correct, expected, wrongGaps, e.explain, answer, corrections)
        if (tally.answer(e, result.correct, inputs)) {
            writes +=
                container.appScope.async {
                    container.learning.answer(e, result.correct).also {
                        if (aiOn.value && GenerationPlan.eligible(Chapters.locate(e.id).section)) {
                            container.generation.refillInBackground()
                        }
                    }
                }
        }
        run.save(tally)
        container.sounds.play(Cue.forAnswer(result.correct))
        _state.value =
            q.copy(
                done = tally.done,
                feedback = result,
                usedWords = tally.usedWords(q.task),
                answerBack = PersonalQuestion.questionIn(e)?.takeIf { correct && aiOn.value }?.let(::AnswerBack),
            )
    }

    fun explain(question: String? = null) {
        val q = _state.value as? SessionUi.Question ?: return
        val f = q.feedback ?: return
        val current = q.explanation
        if (q.exercise.needsAi || current?.loading == true) return
        val chat =
            when {
                current == null -> {
                    openExplanation(q.exercise, f, question) ?: return
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
        tally.next(f.correct)
        if (tally.finished) finish() else show()
    }

    fun skip() {
        val q = _state.value as? SessionUi.Question ?: return
        if (q.feedback != null || q.exercise !is Speak) return
        writes += container.appScope.async { container.learning.skip(q.exercise) }
        tally.skip()
        run.save(tally)
        if (tally.finished) finish() else show()
    }

    private fun show() {
        val e = tally.current
        val task = Chapters.locate(e.id).task
        _state.value =
            SessionUi.Question(e, task, ++attempt, tally.done, tally.total, null, usedWords = tally.usedWords(task))
    }

    private fun finish() {
        run.clear()
        viewModelScope.launch {
            val done = writes.awaitAll().filterNotNull().lastOrNull()
            if (reviewing) container.day.reviewFinished()
            if (done != null) {
                val next = ChapterCompletion.current(Chapters.all, container.chapters.getCompleted())
                container.sounds.play(Cue.forFinish(chapterDone = true))
                _state.value = SessionUi.ChapterDone(done, next)
                return@launch
            }
            val progress = container.progress.getAll()
            val streak = Stats.streak(container.answers.days.first(), container.clock.today()).current
            container.sounds.play(Cue.forFinish(chapterDone = false))
            _state.value =
                SessionUi.Done(
                    firstTry = tally.firstTry,
                    total = tally.total,
                    chapterId = chapter?.id,
                    before = before,
                    after = chapter?.let { Mastery.share(it.exercises, progress) }?.toPercent() ?: 0,
                    mistakes = tally.mistakes.map { it.mistakeText() },
                    streak = streak,
                )
        }
    }
}

private fun Exercise.mistakeText(): String =
    when (this) {
        is Rewrite -> answers.first()
        is Blank -> Checker.check(this, gaps.map { it.first() }).expected
        is Speak -> text
        is Order -> expected()
        is Talk -> goal
        is Write -> prompt
    }

private fun openExplanation(
    exercise: Exercise,
    f: Feedback,
    question: String?,
): Explanation? {
    val loc = Chapters.locate(exercise.id)
    val chat =
        AiChat(
            ExplainPrompt.build(
                loc.chapter,
                loc.section,
                loc.task,
                exercise,
                f.answer,
                f.expected,
                RuleScope.forExercise(Chapters.all, loc.chapter, loc.section),
                correct = f.correct,
            ),
        )
    return when {
        !f.correct -> chat.start()
        question.isNullOrBlank() -> null
        else -> chat.ask(question)
    }
}
