package dev.eftrainer.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SQL миграций — отдельно от классов Room, чтобы JVM-тест сверил его со схемой из app/schemas.
 * Текст обязан совпадать с createSql, который Room кладёт в схему версии 2.
 */
object MigrationSql {
    const val CREATE_GENERATED =
        "CREATE TABLE IF NOT EXISTS `GeneratedExercise` (`id` TEXT NOT NULL, `sectionId` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    const val INDEX_GENERATED_SECTION =
        "CREATE INDEX IF NOT EXISTS `index_GeneratedExercise_sectionId` ON `GeneratedExercise` (`sectionId`)"

    val V1_TO_V2 = listOf(CREATE_GENERATED, INDEX_GENERATED_SECTION)
}

/** v1 → v2: таблица свежих упражнений (Э7.6). Прогресс и пройденные главы не трогаем. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MigrationSql.V1_TO_V2.forEach(db::execSQL)
    }
}
