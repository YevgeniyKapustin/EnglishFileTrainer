package dev.eftrainer.data.db

import dev.eftrainer.data.generated.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Room-testing (инструментальный) здесь не гоняется, поэтому сверяем SQL ручной миграции 1→2
 * с тем, что Room экспортировал в схему версии 2: таблица и индекс должны совпасть до символа.
 */
class MigrationTest {

    @Test fun `migration 1 to 2 creates exactly the schema of version 2`() {
        val file = File("schemas/dev.eftrainer.data.db.AppDatabase/2.json")
        @Suppress("UNCHECKED_CAST")
        val db = (Json.parse(file.readText()) as Map<String, Any?>)["database"] as Map<String, Any?>
        val entity = (db["entities"] as List<Map<String, Any?>>).single { it["tableName"] == "GeneratedExercise" }
        fun sql(s: Any?) = (s as String).replace("\${TABLE_NAME}", "GeneratedExercise")
        val expected = listOf(sql(entity["createSql"])) + (entity["indices"] as List<Map<String, Any?>>).map { sql(it["createSql"]) }
        assertEquals(expected, MigrationSql.V1_TO_V2)
    }

    @Test fun `version 1 tables are untouched`() {
        assert(MigrationSql.V1_TO_V2.none { "Progress" in it || "ChapterState" in it || it.startsWith("DROP") })
    }
}
