package dev.eftrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.eftrainer.data.db.entities.GeneratedExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedDao {
    @Query("SELECT * FROM GeneratedExercise ORDER BY id")
    suspend fun getAll(): List<GeneratedExercise>

    /** То же, но с обновлением после каждой вставки — счётчик Review видит свежие сразу. */
    @Query("SELECT * FROM GeneratedExercise ORDER BY id")
    fun observeAll(): Flow<List<GeneratedExercise>>

    // IGNORE: однажды выданный id не перезаписывается — к нему привязан прогресс
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<GeneratedExercise>)
}
