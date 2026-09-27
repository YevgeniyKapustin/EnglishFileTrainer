package dev.eftrainer.data

import dev.eftrainer.data.db.dao.AnswerDao
import dev.eftrainer.data.db.entities.Answer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class AnswerLog(
    private val dao: AnswerDao,
    scope: CoroutineScope,
) {
    val answers: Flow<List<Answer>> = dao.observeAll()
    val days: SharedFlow<Set<LocalDate>> =
        dao
            .observeDays()
            .map { list -> list.map(LocalDate::ofEpochDay).toSet() }
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val latestDays: Set<LocalDate>? get() = days.replayCache.lastOrNull()

    val firstAnswered: SharedFlow<Map<String, LocalDate>> =
        dao
            .observeFirstAnswers()
            .map { list -> list.associate { it.exerciseId to it.day } }
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

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
