package dev.eftrainer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.eftrainer.data.db.dao.AnswerDao
import dev.eftrainer.data.db.dao.ChapterStateDao
import dev.eftrainer.data.db.dao.GeneratedDao
import dev.eftrainer.data.db.dao.ProgressDao
import dev.eftrainer.data.db.dao.SkippedDao
import dev.eftrainer.data.db.entities.Answer
import dev.eftrainer.data.db.entities.ChapterState
import dev.eftrainer.data.db.entities.GeneratedExercise
import dev.eftrainer.data.db.entities.Progress
import dev.eftrainer.data.db.entities.SkippedExercise
import java.time.LocalDate

@Database(
    entities = [Progress::class, ChapterState::class, GeneratedExercise::class, Answer::class, SkippedExercise::class],
    version = 4,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    abstract fun chapterStateDao(): ChapterStateDao

    abstract fun generatedDao(): GeneratedDao

    abstract fun answerDao(): AnswerDao

    abstract fun skippedDao(): SkippedDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room
                .databaseBuilder(context, AppDatabase::class.java, "trainer.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}

class Converters {
    @TypeConverter fun fromDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter fun toDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}
