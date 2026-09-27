package dev.eftrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.eftrainer.data.db.entities.SkippedExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface SkippedDao {
    @Query("SELECT exerciseId FROM SkippedExercise")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT exerciseId FROM SkippedExercise")
    suspend fun getIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(skipped: SkippedExercise)
}
