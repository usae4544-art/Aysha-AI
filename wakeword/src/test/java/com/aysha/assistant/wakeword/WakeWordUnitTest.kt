package com.aysha.assistant.wakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WakeWordUnitTest {

    @Test
    fun `wake phrase matcher detects exact and phonetic aysha wake words`() {
        assertEquals("hey aysha", WakePhraseMatcher.matchTranscript("Hey Aysha"))
        assertEquals("hello aysha", WakePhraseMatcher.matchTranscript("hello aysha open youtube"))
        assertEquals("wake up aysha", WakePhraseMatcher.matchTranscript("wake up aysha"))
        assertNotNull(WakePhraseMatcher.matchTranscript("hey aisha"))
        assertNotNull(WakePhraseMatcher.matchTranscript("hello asha"))
        assertNull(WakePhraseMatcher.matchTranscript("open calculator please"))
    }

    @Test
    fun `wake phrase matcher honors custom assistant name`() {
        val hit = WakePhraseMatcher.matchTranscript(
            transcript = "hey nova start study mode",
            configuredCsv = "Hey Nova",
            assistantName = "Nova",
        )
        assertEquals("hey nova", hit)
    }
}
