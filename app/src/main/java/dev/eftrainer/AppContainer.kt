package dev.eftrainer

import android.content.Context
import androidx.work.ExistingWorkPolicy
import dev.eftrainer.core.time.Clock
import dev.eftrainer.core.time.SystemClock
import dev.eftrainer.data.AnswerLog
import dev.eftrainer.data.ChapterRepository
import dev.eftrainer.data.ProgressRepository
import dev.eftrainer.data.SkipLog
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiSettings
import dev.eftrainer.data.ai.Connectivity
import dev.eftrainer.data.ai.aiAvailable
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.db.AppDatabase
import dev.eftrainer.data.generated.GeneratedRepository
import dev.eftrainer.data.settings.DaySettings
import dev.eftrainer.data.settings.ReminderSettings
import dev.eftrainer.data.settings.TaskRuns
import dev.eftrainer.data.settings.ThemeSettings
import dev.eftrainer.data.settings.VoiceSettings
import dev.eftrainer.domain.learning.ChapterCompletion
import dev.eftrainer.domain.learning.DaySource
import dev.eftrainer.domain.learning.Generation
import dev.eftrainer.domain.learning.Learning
import dev.eftrainer.domain.learning.Nudge
import dev.eftrainer.domain.learning.Reminder
import dev.eftrainer.domain.learning.ReviewSource
import dev.eftrainer.reminder.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(
    private val context: Context,
    val clock: Clock = SystemClock,
) {
    private val db = AppDatabase.create(context)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val progress = ProgressRepository(db.progressDao())
    val chapters = ChapterRepository(db.chapterStateDao())
    val answers = AnswerLog(db.answerDao(), appScope)
    val skips = SkipLog(db.skippedDao())
    val learning = Learning(progress, chapters, clock, answers, skips)
    val ai = AiSettings(context)
    val aiClient = AiClient(ai)
    val connectivity = Connectivity(context)
    val generated = GeneratedRepository(db.generatedDao())
    val generation = Generation(generated, progress, chapters, aiClient)
    val aiOn = aiAvailable(ai, connectivity)
    val review = ReviewSource(chapters, progress, generated, aiOn, clock, appScope)
    val day = DaySource(review, Chapters.all, answers, skips, DaySettings(context), clock, appScope)
    val theme = ThemeSettings(context)
    val voice = VoiceSettings(context)
    val reminder = ReminderSettings(context)
    val taskRuns = TaskRuns(context)

    init {
        appScope.launch { learning.syncCompleted() }
    }

    fun reschedule() =
        Reminders.schedule(context, reminder.enabled.value, reminder.time.value, ExistingWorkPolicy.REPLACE)

    suspend fun reminderNudge(): Nudge? {
        val inputs = review.inputs.first()
        return Reminder.nudge(
            inputs.progress,
            review.plan(inputs),
            ChapterCompletion.current(Chapters.all, inputs.completed),
            clock.today(),
        )
    }
}
