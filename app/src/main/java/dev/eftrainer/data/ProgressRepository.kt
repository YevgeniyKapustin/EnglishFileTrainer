package dev.eftrainer.data

import dev.eftrainer.data.db.dao.ProgressDao
import dev.eftrainer.data.db.entities.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepository(private val dao: ProgressDao) {

    /** Весь прогресс, ключ — id упражнения. Обновляется после каждого ответа. */
    val progress: Flow<Map<String, Progress>> = dao.observeAll().map { list -> list.associateBy { it.exerciseId } }

    suspend fun get(exerciseId: String): Progress? = dao.get(exerciseId)

    suspend fun getAll(): Map<String, Progress> = dao.getAll().associateBy { it.exerciseId }

    suspend fun save(progress: Progress) = dao.upsert(progress)
}
