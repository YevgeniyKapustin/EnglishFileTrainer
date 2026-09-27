package dev.eftrainer.data.db

import dev.eftrainer.data.generated.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MigrationTest {
    private fun schemaOf(
        version: Int,
        table: String,
    ): List<String> {
        val file = File("schemas/dev.eftrainer.data.db.AppDatabase/$version.json")
        val db = (Json.parse(file.readText()) as Map<*, *>)["database"] as Map<*, *>
        val entity = (db["entities"] as List<*>).filterIsInstance<Map<*, *>>().single { it["tableName"] == table }

        fun sql(s: Any?) = (s as String).replace("\${TABLE_NAME}", table)
        return listOf(sql(entity["createSql"])) +
            (entity["indices"] as List<*>).filterIsInstance<Map<*, *>>().map { sql(it["createSql"]) }
    }

    @Test
    fun `migration 1 to 2 creates exactly the schema of version 2`() {
        assertEquals(schemaOf(2, "GeneratedExercise"), MigrationSql.V1_TO_V2)
    }

    @Test
    fun `migration 2 to 3 creates exactly the schema of version 3`() {
        assertEquals(schemaOf(3, "Answer"), MigrationSql.V2_TO_V3)
    }

    @Test fun `earlier tables are untouched`() {
        assert(MigrationSql.V1_TO_V2.none { "Progress" in it || "ChapterState" in it || it.startsWith("DROP") })
        assert(MigrationSql.V2_TO_V3.all { "`Answer`" in it && !it.startsWith("DROP") })
    }
}
