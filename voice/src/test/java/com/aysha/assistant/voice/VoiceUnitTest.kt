package com.aysha.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceUnitTest {

    @Test
    fun `speech formatter strips markdown and links`() {
        val cleaned = SpeechTextFormatter.cleanForSpeech("**Hello** _Aysha_! Check https://example.com #1")
        assertEquals("Hello Aysha ! Check link 1", cleaned)
    }

    @Test
    fun `speech chunking splits long text cleanly on boundaries`() {
        val longText = List(40) { "This is sentence number $it for testing chunking." }.joinToString(" ")
        val chunks = SpeechTextFormatter.splitChunks(longText, maxChars = 400)
        assertTrue(chunks.size > 2)
        assertTrue(chunks.all { it.length <= 420 })
    }
}
