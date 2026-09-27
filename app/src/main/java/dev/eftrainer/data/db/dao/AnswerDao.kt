package dev.eftrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.eftrainer.data.db.entities.Answer
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Query("SELECT * FROM Answer ORDER BY second")
    fun observeAll(): Flow<List<Answer>>

    @Query("SELECT DISTINCT day FROM Answer")
    fun observeDays(): Flow<List<Long>>

    @Insert
    suspend fun insert(answer: Answer)
}
