package dev.eftrainer.data.db

import dev.eftrainer.data.generated.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MigrationTest {
    @Test
    fun `migration 1 to 2 creates exactly the schema of version 2`() {
        val file = File("schemas/dev.eftrainer.data.db.AppDatabase/2.json")
        val db = (Json.parse(file.readText()) as Map<*, *>)["database"] as Map<*, *>
        val entity =
            (db["entities"] as List<*>).filterIsInstance<Map<*, *>>().single {
                it["tableName"] ==
                    "GeneratedExercise"
            }

        fun sql(s: Any?) = (s as String).replace("\${TABLE_NAME}", "GeneratedExercise")
        val expected =
            listOf(
                sql(entity["createSql"]),
            ) +
                (entity["indices"] as List<*>).filterIsInstance<Map<*, *>>().map { sql(it["createSql"]) }
        assertEquals(expected, MigrationSql.V1_TO_V2)
    }

    @Test fun `version 1 tables are untouched`() {
        assert(MigrationSql.V1_TO_V2.none { "Progress" in it || "ChapterState" in it || it.startsWith("DROP") })
    }
}
