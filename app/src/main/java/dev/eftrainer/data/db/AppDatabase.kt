package dev.eftrainer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.eftrainer.data.db.dao.ChapterStateDao
import dev.eftrainer.data.db.dao.GeneratedDao
import dev.eftrainer.data.db.dao.ProgressDao
import dev.eftrainer.data.db.entities.ChapterState
import dev.eftrainer.data.db.entities.GeneratedExercise
import dev.eftrainer.data.db.entities.Progress
import java.time.LocalDate

// Меняешь сущности — поднимай version и пиши миграцию (схемы лежат в app/schemas).
// fallbackToDestructiveMigration не используем: он молча сотрёт прогресс.
// v2 — GeneratedExercise (свежие упражнения ИИ), миграция MIGRATION_1_2 в Migrations.kt.
@Database(entities = [Progress::class, ChapterState::class, GeneratedExercise::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun chapterStateDao(): ChapterStateDao
    abstract fun generatedDao(): GeneratedDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "trainer.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

class Converters {
    @TypeConverter fun fromDate(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun toDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}
