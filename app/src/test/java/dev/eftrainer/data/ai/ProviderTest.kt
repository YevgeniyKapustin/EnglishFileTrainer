package dev.eftrainer.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderTest {
    private val hex32 = "0123456789abcdef0123456789abcdef"

    @Test fun `empty or null key is no provider`() {
        assertNull(detectProvider(null))
        assertNull(detectProvider(""))
        assertNull(detectProvider("   "))
    }

    @Test fun `claude key`() = assertEquals(Provider.CLAUDE, detectProvider("sk-ant-api03-abc_DEF-123"))

    @Test fun `openrouter key`() = assertEquals(Provider.OPENROUTER, detectProvider("sk-or-v1-$hex32$hex32"))

    @Test fun `gemini key`() = assertEquals(Provider.GEMINI, detectProvider("AIzaSyA-abcdefghijklmnopqrstuvwxyz0123"))

    @Test fun `deepseek key is sk and 32 lowercase hex`() = assertEquals(Provider.DEEPSEEK, detectProvider("sk-$hex32"))

    @Test fun `other sk keys are openai`() {
        assertEquals(Provider.OPENAI, detectProvider("sk-proj-Abc123_xyz"))
        assertEquals(Provider.OPENAI, detectProvider("sk-${hex32.uppercase()}"))
        assertEquals(Provider.OPENAI, detectProvider("sk-${hex32}0"))
    }

    @Test fun `whitespace is trimmed`() {
        assertEquals(Provider.DEEPSEEK, detectProvider("  sk-$hex32\n"))
        assertEquals(Provider.CLAUDE, detectProvider("\tsk-ant-xyz "))
    }

    @Test fun `garbage is no provider`() {
        assertNull(detectProvider("hello"))
        assertNull(detectProvider("SK-abc"))
        assertNull(detectProvider("aiza123"))
    }

    @Test fun `claude uses x-api-key, others bearer`() {
        val c = headers(Provider.CLAUDE, "k")
        assertEquals("k", c["x-api-key"])
        assertEquals("2023-06-01", c["anthropic-version"])
        assertNull(c["Authorization"])
        Provider.entries.filter { it != Provider.CLAUDE }.forEach {
            assertEquals("Bearer k", headers(it, "k")["Authorization"])
        }
    }

    @Test fun `system messages are split out for claude`() {
        val (system, rest) = splitSystem(listOf("system" to "a", "user" to "q", "system" to "b", "assistant" to "r"))
        assertEquals("a\n\nb", system)
        assertEquals(listOf("user" to "q", "assistant" to "r"), rest)
        assertNull(splitSystem(listOf("user" to "q")).first)
    }

    @Test fun `response codes map to reasons`() {
        assertNull(failureFor(Provider.OPENAI, 200))
        listOf(401, 402, 403).forEach { assertEquals(AiException.Reason.BAD_KEY, failureFor(Provider.DEEPSEEK, it)) }
        assertEquals(AiException.Reason.BAD_KEY, failureFor(Provider.GEMINI, 400))
        assertEquals(AiException.Reason.SERVER, failureFor(Provider.OPENAI, 400))
        assertEquals(AiException.Reason.SERVER, failureFor(Provider.CLAUDE, 529))
    }
}
