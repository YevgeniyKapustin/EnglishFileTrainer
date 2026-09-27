package dev.eftrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.eftrainer.data.db.entities.Progress
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM Progress")
    fun observeAll(): Flow<List<Progress>>

    @Query("SELECT * FROM Progress WHERE exerciseId = :exerciseId")
    suspend fun get(exerciseId: String): Progress?

    @Query("SELECT * FROM Progress")
    suspend fun getAll(): List<Progress>

    @Upsert
    suspend fun upsert(progress: Progress)
}
