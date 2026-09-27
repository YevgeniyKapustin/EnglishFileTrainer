package dev.eftrainer

import android.content.Context
import dev.eftrainer.core.time.Clock
import dev.eftrainer.core.time.SystemClock
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiSettings
import dev.eftrainer.data.ai.Connectivity
import dev.eftrainer.data.db.AppDatabase
import dev.eftrainer.data.generated.GeneratedRepository
import dev.eftrainer.data.settings.ThemeSettings
import dev.eftrainer.data.settings.VoiceSettings
import dev.eftrainer.domain.learning.Generation
import dev.eftrainer.domain.learning.Learning
import dev.eftrainer.domain.learning.ReviewSource
import dev.eftrainer.data.ai.aiAvailable

/** Ручной DI: всё создаётся один раз на процесс (plans/architecture.md). */
class AppContainer(context: Context, val clock: Clock = SystemClock) {
    private val db = AppDatabase.create(context)

    val progress = ProgressRepository(db.progressDao())
    val chapters = ChapterRepository(db.chapterStateDao())
    val learning = Learning(progress, chapters, clock)
    val ai = AiSettings(context)
    val aiClient = AiClient(ai)
    val connectivity = Connectivity(context)
    val generated = GeneratedRepository(db.generatedDao())                       // Э7.6 свежие упражнения
    val generation = Generation(generated, progress, chapters, aiClient)
    val aiOn = aiAvailable(ai, connectivity)                                       // ключ + сеть
    val review = ReviewSource(chapters, progress, generated, aiOn, clock)          // состав Review: счётчик = сессия
    val theme = ThemeSettings(context)
    val voice = VoiceSettings(context)                                             // озвучка разговора
}
