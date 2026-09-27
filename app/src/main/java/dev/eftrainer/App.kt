package dev.eftrainer

import android.app.Application

class App : Application() {
    val container by lazy { AppContainer(this) }
}
