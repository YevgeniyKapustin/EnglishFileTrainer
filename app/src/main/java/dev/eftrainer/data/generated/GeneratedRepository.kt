package dev.eftrainer.data.generated

import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.db.dao.GeneratedDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeneratedRepository(
    private val dao: GeneratedDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun getAll(): List<GeneratedItem> =
        dao.getAll().mapNotNull(GeneratedCodec::decode).also(Chapters::registerGenerated)

    suspend fun bySection(): Map<String, List<Exercise>> = getAll().groupBy({ it.sectionId }, { it.exercise })

    fun observeBySection(): Flow<Map<String, List<Exercise>>> =
        dao.observeAll().map { rows ->
            rows
                .mapNotNull(
                    GeneratedCodec::decode,
                ).also(Chapters::registerGenerated)
                .groupBy({ it.sectionId }, { it.exercise })
        }

    suspend fun add(
        sectionId: String,
        items: List<GeneratedItem>,
    ): List<GeneratedItem> {
        if (items.isEmpty()) return emptyList()
        val ids = GeneratedIds.next(sectionId, dao.getAll().map { it.id }, items.size)
        val saved = items.zip(ids) { item, id -> item.copy(sectionId = sectionId, exercise = item.exercise.withId(id)) }
        val at = now()
        dao.insertAll(saved.map { GeneratedCodec.encode(it, at) })
        Chapters.registerGenerated(saved)
        return saved
    }

    private fun Exercise.withId(id: String): Exercise =
        when (this) {
            is Rewrite -> copy(id = id)
            is Blank -> copy(id = id)
            else -> throw IllegalArgumentException("сгенерировать можно только Rewrite/Blank")
        }
}
