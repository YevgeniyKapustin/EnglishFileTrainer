package dev.eftrainer.data.settings

import dev.eftrainer.domain.learning.TaskRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskRunCodecTest {
    @Test fun `run survives a round trip`() {
        val run = TaskRun(mapOf("1A-g-1" to listOf("am", "\"not\" is")), setOf("1A-g-2"), setOf("1A-p-3"))
        assertEquals(run, TaskRunCodec.decode(TaskRunCodec.encode(run)))
    }

    @Test fun `broken text decodes to nothing`() {
        assertNull(TaskRunCodec.decode("{\"passed\":"))
    }
}
