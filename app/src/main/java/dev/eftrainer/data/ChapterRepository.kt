package dev.eftrainer.data

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.db.dao.ChapterStateDao
import dev.eftrainer.data.db.entities.ChapterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ChapterRepository(private val dao: ChapterStateDao) {

    val chapters: List<Chapter> = Chapters.all

    /** id пройденных глав. */
    val completed: Flow<Set<String>> = dao.observeAll().map { list -> list.mapTo(mutableSetOf()) { it.chapterId } }

    suspend fun getCompleted(): Set<String> = dao.getAll().mapTo(mutableSetOf()) { it.chapterId }

    suspend fun markCompleted(chapterId: String, on: LocalDate) = dao.insert(ChapterState(chapterId, on))
}
