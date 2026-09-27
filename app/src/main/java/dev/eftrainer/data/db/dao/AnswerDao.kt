package dev.eftrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.eftrainer.data.db.entities.Answer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class FirstAnswer(
    val exerciseId: String,
    val day: LocalDate,
)

@Dao
interface AnswerDao {
    @Query("SELECT * FROM Answer ORDER BY second")
    fun observeAll(): Flow<List<Answer>>

    @Query("SELECT DISTINCT day FROM Answer")
    fun observeDays(): Flow<List<Long>>

    @Query("SELECT exerciseId, MIN(day) AS day FROM Answer GROUP BY exerciseId")
    fun observeFirstAnswers(): Flow<List<FirstAnswer>>

    @Insert
    suspend fun insert(answer: Answer)
}
