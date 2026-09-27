package dev.eftrainer.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.eftrainer.App
import dev.eftrainer.domain.learning.Reminder
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val WORK = "daily-reminder"

    fun schedule(
        context: Context,
        enabled: Boolean,
        at: LocalTime,
        policy: ExistingWorkPolicy,
    ) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK)
            return
        }
        val delay = Reminder.delayUntil(LocalDateTime.now(), at)
        val request =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
        work.enqueueUniqueWork(WORK, policy, request)
    }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as App).container
        container.reminderNudge()?.let { Notifier.post(applicationContext, it) }
        Reminders.schedule(
            applicationContext,
            container.reminder.enabled.value,
            container.reminder.time.value,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
        )
        return Result.success()
    }
}
