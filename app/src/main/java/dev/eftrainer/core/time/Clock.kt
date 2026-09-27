package dev.eftrainer.core.time

import java.time.LocalDate

fun interface Clock {
    fun today(): LocalDate
}

object SystemClock : Clock {
    override fun today(): LocalDate = LocalDate.now()
}
