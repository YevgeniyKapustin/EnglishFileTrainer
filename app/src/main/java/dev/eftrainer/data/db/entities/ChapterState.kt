package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Глава пройдена — фиксируем навсегда, даже если потом процент освоения просядет.
 * Текущая глава = первая без записи здесь (plans/learning.md).
 */
@Entity
data class ChapterState(
    @PrimaryKey val chapterId: String,
    val completedOn: LocalDate,
)
