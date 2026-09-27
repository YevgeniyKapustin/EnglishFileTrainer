package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(indices = [Index("day")])
data class Answer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val day: LocalDate,
    val second: Long,
    val correct: Boolean,
)
