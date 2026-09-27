package dev.eftrainer.domain.learning

import dev.eftrainer.data.content.Chapter
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Section
import dev.eftrainer.data.content.SectionKind
import dev.eftrainer.data.content.Task
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

val TODAY: LocalDate = LocalDate.of(2026, 3, 10)

fun section(
    id: String,
    count: Int,
) = Section(
    id = id,
    kind = SectionKind.GRAMMAR,
    title = id,
    reference = emptyList(),
    tasks =
        listOf(
            Task("$id-task", Origin.WB, "Do it", (1..count).map { Rewrite("$id-$it", "p", listOf("a")) }),
        ),
)

fun chapter(
    id: String,
    vararg sections: Section,
) = Chapter(id, id, sections.toList())

fun progress(
    id: String,
    streak: Int = 0,
    dueOn: LocalDate = TODAY,
    lastAnsweredOn: LocalDate = TODAY.minusDays(1),
) = Progress(
    id,
    streak,
    stability = 1.0,
    difficulty = 5.0,
    dueOn = dueOn,
    lastAnsweredOn = lastAnsweredOn,
    correctCount = streak,
    wrongCount = 0,
)

fun mastered(
    id: String,
    lastAnsweredOn: LocalDate = TODAY.minusDays(1),
    dueOn: LocalDate = TODAY.plusDays(30),
) = progress(id, streak = 3, dueOn = dueOn, lastAnsweredOn = lastAnsweredOn)

fun Iterable<Progress>.byId(): Map<String, Progress> = associateBy { it.exerciseId }

fun Section.withPresets(count: Int) =
    copy(
        tasks =
            tasks +
                Task(
                    "$id-x1",
                    Origin.GENERATED,
                    "Extra",
                    (1..count).map { Rewrite("$id-x1-" + it.toString().padStart(2, '0'), "p$it", listOf("x$it")) },
                ),
    )
