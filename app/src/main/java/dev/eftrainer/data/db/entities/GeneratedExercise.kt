package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("sectionId")])
data class GeneratedExercise(
    @PrimaryKey val id: String,
    val sectionId: String,
    val type: String,
    val payload: String,
    val createdAt: Long,
)
