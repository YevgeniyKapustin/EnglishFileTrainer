package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.presets
import dev.eftrainer.data.content.aiExercises
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

/** Сессия Review: сначала due из пройденных глав, потом текущая глава. */
data class ReviewPlan(
    val due: List<Exercise>,       // из пройденных глав
    val current: List<Exercise>,   // из текущей
) {
    val all: List<Exercise> get() = due + current
}

/** Что показывать в сессии (plans/learning.md). Чистые функции, даты — снаружи. */
object TrainingQueue {
    const val SESSION_SIZE = 20
    const val DUE_LIMIT = 10
    const val MASTERED_SHARE = 5   // освоенное — не больше 1 из 5 мест

    /** Первый проход раздела: ещё не отвеченные, в порядке книги. Упражнения только с ИИ (разговор, письменный ответ) — только если [ai]. */
    fun firstPass(section: Section, progress: Map<String, Progress>, ai: Boolean = false): List<Exercise> =
        section.tasks.flatMap { it.exercises }
            .filter { it in section.exercises || (ai && it in section.aiExercises) }
            .filter { it.id !in progress }

    /** Свежие упражнения раздела: сначала заготовки из приложения, потом досочинённые ИИ. */
    fun freshOf(section: Section, generated: Map<String, List<Exercise>>): List<Exercise> =
        section.presets + generated[section.id].orEmpty()

    fun review(
        chapters: List<Chapter>,
        completed: Set<String>,
        progress: Map<String, Progress>,
        today: LocalDate,
        ai: Boolean = false,
        generated: Map<String, List<Exercise>> = emptyMap(),   // свежие упражнения ИИ по id раздела (заготовки — в Section)
    ): ReviewPlan {
        fun answeredToday(e: Exercise) = progress[e.id]?.lastAnsweredOn == today

        // Пройденные главы — по датам FSRS, самое просроченное первым. Отвеченные свежие — так же.
        val due = chapters.filter { it.id in completed }
            .flatMap { c -> c.exercises + c.sections.flatMap { freshOf(it, generated) } }
            .filter { e -> progress[e.id]?.let { it.dueOn <= today } == true && !answeredToday(e) }
            .sortedBy { progress.getValue(it.id).dueOn }
            .take(DUE_LIMIT)

        val chapter = ChapterCompletion.current(chapters, completed) ?: return ReviewPlan(due, emptyList())
        val room = SESSION_SIZE - due.size

        // Текущая глава — без дат. Отвеченное сегодня пропускаем: серия сегодня уже не вырастет.
        val open = ChapterCompletion.activeSections(chapter, progress)
        val candidates = open
            .flatMap { if (ai) it.exercises + it.aiExercises else it.exercises }
            .filterNot(::answeredToday)
        // Свежие упражнения — только для разделов, где книга уже освоена: правило гоняем дальше на новом.
        // Заготовки — раньше досочинённых ИИ (сортировка ниже устойчивая).
        val fresh = open.filter { GenerationPlan.bookMastered(it, progress) }
            .flatMap { freshOf(it, generated) }
            .filterNot(::answeredToday)
        val (mastered, unmastered) = candidates.partition { Mastery.isMastered(progress[it.id]) }
        val (freshMastered, freshUnmastered) = fresh.partition { Mastery.isMastered(progress[it.id]) }

        // Неосвоенное: сначала уже отвеченное со слабой серией, новое — в конце, в порядке книги.
        val weakFirst = unmastered.sortedWith(
            compareBy<Exercise>({ it.id !in progress }, { progress[it.id]?.streak ?: 0 }, { progress[it.id]?.lastAnsweredOn }),
        )
        // Свежие — после книжного неосвоенного: сначала ещё не отвеченные (заготовки, потом ИИ по порядку создания), потом слабые.
        val freshFirst = freshUnmastered.sortedWith(
            compareBy<Exercise>({ it.id in progress }, { progress[it.id]?.streak ?: 0 }, { progress[it.id]?.lastAnsweredOn }),
        )
        val oldestMastered = (mastered + freshMastered).sortedBy { progress.getValue(it.id).lastAnsweredOn }

        val mixCount = minOf(oldestMastered.size, room / MASTERED_SHARE)
        val main = (weakFirst + freshFirst).take(room - mixCount)
        val mix = oldestMastered.take(mixCount)

        return ReviewPlan(due, interleave(main, mix))
    }

    /** Вставляет [mix] равномерно между элементами [main]. */
    private fun interleave(main: List<Exercise>, mix: List<Exercise>): List<Exercise> {
        if (mix.isEmpty()) return main
        val step = main.size / (mix.size + 1) + 1
        val result = mutableListOf<Exercise>()
        var m = 0
        main.forEachIndexed { i, e ->
            result += e
            if ((i + 1) % step == 0 && m < mix.size) result += mix[m++]
        }
        while (m < mix.size) result += mix[m++]
        return result
    }
}
