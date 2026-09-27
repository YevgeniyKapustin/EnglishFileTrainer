package dev.eftrainer.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MigrationSql {
    const val CREATE_GENERATED =
        "CREATE TABLE IF NOT EXISTS `GeneratedExercise` (`id` TEXT NOT NULL, `sectionId` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    const val INDEX_GENERATED_SECTION =
        "CREATE INDEX IF NOT EXISTS `index_GeneratedExercise_sectionId` ON `GeneratedExercise` (`sectionId`)"

    val V1_TO_V2 = listOf(CREATE_GENERATED, INDEX_GENERATED_SECTION)

    const val CREATE_ANSWER =
        "CREATE TABLE IF NOT EXISTS `Answer` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`exerciseId` TEXT NOT NULL, `day` INTEGER NOT NULL, `second` INTEGER NOT NULL, `correct` INTEGER NOT NULL)"
    const val INDEX_ANSWER_DAY = "CREATE INDEX IF NOT EXISTS `index_Answer_day` ON `Answer` (`day`)"

    val V2_TO_V3 = listOf(CREATE_ANSWER, INDEX_ANSWER_DAY)
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MigrationSql.V1_TO_V2.forEach(db::execSQL)
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MigrationSql.V2_TO_V3.forEach(db::execSQL)
        }
    }
