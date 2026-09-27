package dev.eftrainer.data

import dev.eftrainer.data.db.dao.AnswerDao
import dev.eftrainer.data.db.entities.Answer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class AnswerLog(
    private val dao: AnswerDao,
) {
    val answers: Flow<List<Answer>> = dao.observeAll()
    val days: Flow<Set<LocalDate>> = dao.observeDays().map { list -> list.map(LocalDate::ofEpochDay).toSet() }

    suspend fun add(
        exerciseId: String,
        at: LocalDateTime,
        correct: Boolean,
    ) = dao.insert(
        Answer(
            exerciseId = exerciseId,
            day = at.toLocalDate(),
            second = at.toEpochSecond(ZoneOffset.UTC),
            correct = correct,
        ),
    )
}
