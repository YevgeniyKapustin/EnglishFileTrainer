package dev.eftrainer.data

import dev.eftrainer.data.db.dao.SkippedDao
import dev.eftrainer.data.db.entities.SkippedExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SkipLog(
    private val dao: SkippedDao,
) {
    val skipped: Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    suspend fun getAll(): Set<String> = dao.getIds().toSet()

    suspend fun add(exerciseId: String) = dao.insert(SkippedExercise(exerciseId))
}
