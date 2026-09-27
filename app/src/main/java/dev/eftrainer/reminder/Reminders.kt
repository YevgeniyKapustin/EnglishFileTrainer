package dev.eftrainer.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.eftrainer.App
import dev.eftrainer.MainActivity
import dev.eftrainer.R
import dev.eftrainer.domain.learning.Nudge
import dev.eftrainer.domain.learning.Reminder
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val WORK = "daily-reminder"
    private const val CHANNEL = "reminder"
    private const val NOTIFICATION = 1

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

    fun canPost(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    internal fun post(
        context: Context,
        nudge: Nudge,
    ) {
        if (!canPost(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.reminder),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text(context, nudge))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        manager.notify(NOTIFICATION, notification)
    }

    private fun text(
        context: Context,
        nudge: Nudge,
    ): String =
        when (nudge) {
            is Nudge.Review -> {
                context.resources.getQuantityString(R.plurals.reminder_review, nudge.count, nudge.count)
            }

            is Nudge.Continue -> {
                context.getString(R.string.reminder_continue, nudge.chapter.id, nudge.chapter.title)
            }
        }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as App).container
        container.reminderNudge()?.let { Reminders.post(applicationContext, it) }
        Reminders.schedule(
            applicationContext,
            container.reminder.enabled.value,
            container.reminder.time.value,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
        )
        return Result.success()
    }
}
