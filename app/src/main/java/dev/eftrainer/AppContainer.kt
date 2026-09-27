package dev.eftrainer

import android.content.Context
import dev.eftrainer.core.time.Clock
import dev.eftrainer.core.time.SystemClock
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiSettings
import dev.eftrainer.data.ai.Connectivity
import dev.eftrainer.data.ai.aiAvailable
import dev.eftrainer.data.db.AppDatabase
import dev.eftrainer.data.generated.GeneratedRepository
import dev.eftrainer.data.settings.ThemeSettings
import dev.eftrainer.data.settings.VoiceSettings
import dev.eftrainer.domain.learning.Generation
import dev.eftrainer.domain.learning.Learning
import dev.eftrainer.domain.learning.ReviewSource

class AppContainer(
    context: Context,
    val clock: Clock = SystemClock,
) {
    private val db = AppDatabase.create(context)

    val progress = ProgressRepository(db.progressDao())
    val chapters = ChapterRepository(db.chapterStateDao())
    val learning = Learning(progress, chapters, clock)
    val ai = AiSettings(context)
    val aiClient = AiClient(ai)
    val connectivity = Connectivity(context)
    val generated = GeneratedRepository(db.generatedDao())
    val generation = Generation(generated, progress, chapters, aiClient)
    val aiOn = aiAvailable(ai, connectivity)
    val review = ReviewSource(chapters, progress, generated, aiOn, clock)
    val theme = ThemeSettings(context)
    val voice = VoiceSettings(context)
}
