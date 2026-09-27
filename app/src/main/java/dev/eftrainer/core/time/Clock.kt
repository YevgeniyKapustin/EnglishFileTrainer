package dev.eftrainer.core.time

import java.time.LocalDate

/** Источник «сегодня». В тестах подменяется, чтобы проверять повторение по дням. */
fun interface Clock {
    fun today(): LocalDate
}

object SystemClock : Clock {
    override fun today(): LocalDate = LocalDate.now()
}
