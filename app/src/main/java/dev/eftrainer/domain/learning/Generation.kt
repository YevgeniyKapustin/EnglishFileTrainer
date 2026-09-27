package dev.eftrainer.domain.learning

import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.SectionKind
import dev.eftrainer.data.content.exercises
import dev.eftrainer.data.content.presets
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.data.generated.GeneratedRepository
import dev.eftrainer.domain.ai.GeneratePrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Когда и сколько свежих упражнений нужно (plans/learning.md, «Свежие упражнения на правило»). Чистые функции. */
object GenerationPlan {
    const val STOCK = 15   // держим столько неотвеченных сгенерированных на раздел
    const val LOW = 8      // меньше — досочиняем до STOCK

    /** Сгенерированное подключается к разделу, когда все его книжные упражнения освоены. */
    fun bookMastered(section: Section, progress: Map<String, Progress>): Boolean =
        section.exercises.isNotEmpty() && section.exercises.all { Mastery.isMastered(progress[it.id]) }

    /** Правило/лексика — только Grammar и Vocabulary; произношение и аудирование ИИ не сочиняет. */
    fun eligible(section: Section): Boolean = section.kind == SectionKind.GRAMMAR || section.kind == SectionKind.VOCABULARY

    /** Заготовки раздела (вшитые в приложение) все уже встречались — пора досочинять ИИ. */
    fun presetsUsed(section: Section, progress: Map<String, Progress>): Boolean =
        section.presets.all { it.id in progress }

    /**
     * Раздел текущей главы → сколько досочинить. Пусто — ничего не нужно.
     * ИИ — только добавка: пока в разделе есть не показанные заготовки, запросов нет.
     */
    fun refill(chapter: Chapter, progress: Map<String, Progress>, generated: Map<String, List<Exercise>>): List<Pair<Section, Int>> =
        ChapterCompletion.openSections(chapter, progress)
            .filter { eligible(it) && bookMastered(it, progress) && presetsUsed(it, progress) }
            .mapNotNull { s ->
                val stock = generated[s.id].orEmpty().count { it.id !in progress }
                if (stock < LOW) s to (STOCK - stock) else null
            }
}

/**
 * Досочиняет свежие упражнения в фоне (своя область корутин на весь процесс, не держит экран).
 * Ошибки ИИ/сети молча глотаются — попробуем при следующей сессии.
 */
class Generation(
    private val generated: GeneratedRepository,
    private val progress: ProgressRepository,
    private val chapters: ChapterRepository,
    private val ai: AiClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    // Зовётся и после ответов: после неудачи (нет сети, ИИ несёт не то) не дёргаем ИИ на каждом ответе.
    @Volatile private var retryAfter = 0L

    /**
     * Вызывать, только когда ИИ включён. Уже идёт генерация или недавно не вышло — не запускаем.
     * Ничего не нужно (книга раздела не освоена, свежих хватает) — запросов к ИИ нет.
     */
    fun refillInBackground() {
        if (mutex.isLocked || now() < retryAfter) return
        scope.launch {
            if (!mutex.tryLock()) return@launch
            try {
                refill()
            } catch (e: AiException) {
                retryAfter = now() + COOLDOWN_MS   // нет сети / ключ / сервер — попозже
            } finally {
                mutex.unlock()
            }
        }
    }

    private suspend fun refill() {
        val all = chapters.chapters
        val chapter = ChapterCompletion.current(all, chapters.getCompleted()) ?: return
        val progress = progress.getAll()
        val bySection = generated.bySection()
        val studied = all.take(all.indexOf(chapter) + 1)
        for ((section, need) in GenerationPlan.refill(chapter, progress, bySection)) {
            // Заготовки — тоже «уже было»: ИИ их не повторяет.
            val existing = (section.presets + bySection[section.id].orEmpty()).toMutableList()
            var left = need
            // Небольшими пачками: у части провайдеров ответ ограничен ~600 токенами.
            repeat(MAX_REQUESTS) {
                if (left <= 0) return@repeat
                val count = minOf(BATCH, left)
                val reply = ai.chat(GeneratePrompt.build(chapter, section, studied, section.exercises, existing, count))
                val items = GeneratePrompt.parse(reply, section.id, section.exercises, existing, count)
                if (items.isEmpty()) {   // ИИ несёт не то — не жжём запросы, попробуем позже
                    retryAfter = now() + COOLDOWN_MS
                    return
                }
                existing += generated.add(section.id, items).map { it.exercise }
                left -= items.size
            }
        }
    }

    private companion object {
        const val BATCH = 6
        const val MAX_REQUESTS = 3
        const val COOLDOWN_MS = 60_000L
    }
}
