package dev.eftrainer.domain.learning

import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.SkipLog
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
import dev.eftrainer.domain.ai.GeneratedResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

object GenerationPlan {
    const val STOCK = 15
    const val LOW = 8

    fun bookSeen(
        section: Section,
        progress: Map<String, Progress>,
        skipped: Set<String> = emptySet(),
    ): Boolean = section.exercises.isNotEmpty() && section.exercises.all { it.id in progress || it.id in skipped }

    fun eligible(section: Section): Boolean =
        section.kind == SectionKind.GRAMMAR || section.kind == SectionKind.VOCABULARY

    fun presetsUsed(
        section: Section,
        progress: Map<String, Progress>,
    ): Boolean = section.presets.all { it.id in progress }

    fun refill(
        chapter: Chapter,
        progress: Map<String, Progress>,
        generated: Map<String, List<Exercise>>,
        skipped: Set<String> = emptySet(),
    ): List<Pair<Section, Int>> =
        ChapterCompletion
            .openSections(chapter)
            .filter { eligible(it) && bookSeen(it, progress, skipped) && presetsUsed(it, progress) }
            .mapNotNull { s ->
                val stock = generated[s.id].orEmpty().count { it.id !in progress }
                if (stock < LOW) s to STOCK - stock else null
            }
}

class Generation(
    private val generated: GeneratedRepository,
    private val progress: ProgressRepository,
    private val chapters: ChapterRepository,
    private val skips: SkipLog,
    private val ai: AiClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val arrived = MutableSharedFlow<List<Exercise>>(extraBufferCapacity = ARRIVAL_BUFFER)
    val fresh: SharedFlow<List<Exercise>> = arrived.asSharedFlow()

    @Volatile private var retryAfter = 0L

    fun refillInBackground() {
        if (mutex.isLocked || now() < retryAfter) return
        scope.launch {
            if (!mutex.tryLock()) return@launch
            try {
                refill()
            } catch (expected: AiException) {
                retryAfter = now() + COOLDOWN_MS
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
        for ((section, need) in GenerationPlan.refill(chapter, progress, bySection, skips.getAll())) {
            val existing = (section.presets + bySection[section.id].orEmpty()).toMutableList()
            var left = need
            repeat(MAX_REQUESTS) {
                if (left <= 0) return@repeat
                val count = minOf(BATCH, left)
                val reply = ai.chat(GeneratePrompt.build(chapter, section, studied, section.exercises, existing, count))
                val items = GeneratedResponse.parse(reply, section.id, section.exercises, existing, count)
                if (items.isEmpty()) {
                    retryAfter = now() + COOLDOWN_MS
                    return
                }
                val saved = generated.add(section.id, items).map { it.exercise }
                arrived.tryEmit(saved)
                existing += saved
                left -= items.size
            }
        }
    }

    private companion object {
        const val BATCH = 6
        const val MAX_REQUESTS = 3
        const val COOLDOWN_MS = 60_000L
        const val ARRIVAL_BUFFER = 8
    }
}
