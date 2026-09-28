package dev.eftrainer.data.settings

import dev.eftrainer.domain.learning.DayRun
import dev.eftrainer.domain.learning.TaskRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DayRunCodecTest {
    @Test fun `run survives a round trip`() {
        val run =
            DayRun(
                "continue-1B",
                LocalDate.of(2026, 9, 28),
                listOf("1B-g-1", "1B-g-2", "1B-g-gen-0003"),
                TaskRun(mapOf("1B-g-1" to listOf("\"was\"")), setOf("1B-g-2"), emptySet()),
                2,
            )
        assertEquals(run, DayRunCodec.decode(DayRunCodec.encode(run)))
    }

    @Test fun `broken text decodes to nothing`() {
        assertNull(DayRunCodec.decode("""{"key":"review","date":"yesterday"}"""))
    }
}
