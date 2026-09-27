package dev.eftrainer.data.generated

import dev.eftrainer.data.content.AnswerSource
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.db.entities.GeneratedExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeneratedCodecTest {

    @Test fun `json round trip with escapes`() {
        val v = mapOf("a" to "say \"hi\"\n\\ ok \t’", "b" to listOf(listOf("x", "y"), emptyList<String>()), "c" to null, "d" to true)
        assertEquals(v, Json.parse(Json.write(v)))
    }

    @Test fun `json parses numbers and unicode escapes`() {
        assertEquals(mapOf("n" to -1.5, "s" to "é"), Json.parse(" { \"n\" : -1.5 , \"s\" : \"\\u00e9\" } "))
    }

    @Test(expected = Json.ParseException::class) fun `truncated json fails`() {
        Json.parse("""{"items":[{"type":"blank"""")
    }

    @Test fun `rewrite and blank survive room`() {
        val r = GeneratedItem("1A-grammar", "Put the words in order.",
            Rewrite("1A-grammar-gen-0001", "live / you / where / do", listOf("Where do you live?"), hint = "(x)", source = AnswerSource.MODEL))
        val b = GeneratedItem("1A-grammar", null,
            Blank("1A-grammar-gen-0002", "Where {} born? {} you?", listOf(listOf("were you"), listOf("Are", "are")), source = AnswerSource.MODEL))
        for (item in listOf(r, b)) assertEquals(item, GeneratedCodec.decode(GeneratedCodec.encode(item, 42)))
    }

    @Test fun `broken row is skipped`() {
        assertNull(GeneratedCodec.decode(GeneratedExercise("x-gen-0001", "x", "blank", """{"text":"{} {}","gaps":[["a"]]}""", 0)))
        assertNull(GeneratedCodec.decode(GeneratedExercise("x-gen-0002", "x", "rewrite", "not json", 0)))
        assertNull(GeneratedCodec.decode(GeneratedExercise("x-gen-0003", "x", "speak", "{}", 0)))
    }

    @Test fun `ids are stable and continue after max`() {
        assertEquals("1A-grammar-gen-0001", GeneratedIds.of("1A-grammar", 1))
        assertEquals("1A-grammar", GeneratedIds.sectionOf("1A-grammar-gen-0012"))
        assertNull(GeneratedIds.sectionOf("1A-wb2a-03"))
        val existing = listOf("1A-grammar-gen-0003", "1A-grammar-gen-0010", "1A-vocabulary-gen-0050")
        assertEquals(listOf("1A-grammar-gen-0011", "1A-grammar-gen-0012"), GeneratedIds.next("1A-grammar", existing, 2))
        assertEquals(listOf("1A-other-gen-0001"), GeneratedIds.next("1A-other", existing, 1))
    }
}
