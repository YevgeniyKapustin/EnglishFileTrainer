package dev.eftrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.eftrainer.data.db.entities.ChapterState
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterStateDao {
    @Query("SELECT * FROM ChapterState")
    fun observeAll(): Flow<List<ChapterState>>

    @Query("SELECT * FROM ChapterState")
    suspend fun getAll(): List<ChapterState>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(state: ChapterState)
}
