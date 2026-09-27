package dev.eftrainer.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.eftrainer.MainActivity
import dev.eftrainer.R
import dev.eftrainer.domain.learning.Nudge

object Notifier {
    private const val CHANNEL = "reminder"
    private const val NOTIFICATION = 1

    fun canPost(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(
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
