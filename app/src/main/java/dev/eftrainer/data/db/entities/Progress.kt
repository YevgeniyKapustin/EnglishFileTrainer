package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class Progress(
    @PrimaryKey val exerciseId: String,
    val streak: Int,
    val stability: Double,
    val difficulty: Double,
    val dueOn: LocalDate,
    val lastAnsweredOn: LocalDate,
    val correctCount: Int,
    val wrongCount: Int,
)
