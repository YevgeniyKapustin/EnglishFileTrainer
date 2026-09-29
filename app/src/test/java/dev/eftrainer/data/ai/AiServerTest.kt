package dev.eftrainer.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServerTest {
    @Test fun `configured only with both url and app key`() {
        assertTrue(AiServer("http://10.0.2.2:8080", "key").configured)
        assertFalse(AiServer("", "key").configured)
        assertFalse(AiServer("http://10.0.2.2:8080", " ").configured)
        assertFalse(AiServer("", "").configured)
    }

    @Test fun `chat url appends the endpoint once`() {
        assertEquals("https://ai.example.com/v1/chat", AiServer("https://ai.example.com", "k").chatUrl)
        assertEquals("https://ai.example.com/v1/chat", AiServer(" https://ai.example.com/ ", "k").chatUrl)
    }

    @Test fun `response codes map to reasons`() {
        assertNull(failureFor(200))
        assertEquals(AiException.Reason.UNAVAILABLE, failureFor(401))
        assertEquals(AiException.Reason.LIMIT, failureFor(429))
        listOf(400, 413, 500, 502).forEach { assertEquals(AiException.Reason.SERVER, failureFor(it)) }
    }
}
