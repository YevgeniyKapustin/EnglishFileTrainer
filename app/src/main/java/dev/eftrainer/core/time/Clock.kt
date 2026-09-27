package dev.eftrainer.core.time

import java.time.LocalDate
import java.time.LocalDateTime

fun interface Clock {
    fun today(): LocalDate

    fun now(): LocalDateTime = today().atStartOfDay()
}

object SystemClock : Clock {
    override fun today(): LocalDate = LocalDate.now()

    override fun now(): LocalDateTime = LocalDateTime.now()
}
