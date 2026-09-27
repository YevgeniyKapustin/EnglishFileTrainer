package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class ChapterState(
    @PrimaryKey val chapterId: String,
    val completedOn: LocalDate,
)
