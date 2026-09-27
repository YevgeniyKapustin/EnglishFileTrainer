package dev.eftrainer.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Свежее упражнение, которое ИИ сочинил на правило раздела (plans/learning.md, «Свежие упражнения на правило»).
 * Строки не удаляются: к [id] навсегда привязан прогресс/FSRS, как у книжных.
 */
@Entity(indices = [Index("sectionId")])
data class GeneratedExercise(
    @PrimaryKey val id: String,   // "1A-grammar-gen-0001"
    val sectionId: String,        // "1A-grammar"
    val type: String,             // "rewrite" / "blank"
    val payload: String,          // JSON: текст, ответы, подсказка, инструкция — см. GeneratedCodec
    val createdAt: Long,          // epoch millis
)
