package dev.eftrainer

import android.app.Application
import androidx.work.ExistingWorkPolicy
import dev.eftrainer.reminder.Reminders

class App : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Reminders.schedule(
            this,
            container.reminder.enabled.value,
            container.reminder.time.value,
            ExistingWorkPolicy.KEEP,
        )
    }
}
