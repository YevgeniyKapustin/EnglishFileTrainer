package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Состояние одного упражнения. Нет строки — упражнение новое (plans/learning.md). */
@Entity
data class Progress(
    @PrimaryKey val exerciseId: String,
    val streak: Int,              // верных подряд в разные дни; >= 3 — освоено
    val stability: Double,        // FSRS: через сколько дней шанс вспомнить упадёт до 90%
    val difficulty: Double,       // FSRS: 1..10, насколько упражнение трудное для меня
    val dueOn: LocalDate,         // календарный день, не отметка времени
    val lastAnsweredOn: LocalDate,
    val correctCount: Int,
    val wrongCount: Int,
)
