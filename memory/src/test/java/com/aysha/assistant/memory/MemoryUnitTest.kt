package com.aysha.assistant.memory

import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.MemoryEntry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryUnitTest {

    @Test
    fun `memory matcher ranks exact and approved memories only`() {
        val entries = listOf(
            MemoryEntry(1, "Music", "Enjoys indie acoustic music", "music", approvedByUser = true),
            MemoryEntry(2, "Study", "Prefers 25 min Pomodoro study timer", "study,focus", approvedByUser = true),
            MemoryEntry(3, "Hidden", "Study notes unapproved", "study", approvedByUser = false),
        )
        val ranked = MemoryMatcher.rank(entries, "study timer", limit = 5)
        assertEquals(1, ranked.size)
        assertEquals("Study", ranked.first().title)
    }

    @Test
    fun `chat exporter produces markdown and valid json`() {
        val msgs = listOf(
            ChatMessage(1, ChatRole.USER, "Hello Aysha", "", null, 1700000000000L),
            ChatMessage(2, ChatRole.ASSISTANT, "Hi! How can I help?", "Offline", null, 1700000005000L),
        )
        val mems = listOf(MemoryEntry(1, "City", "Jaipur", "location"))

        val md = ChatExporter.toMarkdown(msgs)
        assertTrue(md.contains("AYSHA Conversation Export"))
        assertTrue(md.contains("Hello Aysha"))

        val jsonStr = ChatExporter.toJson(msgs, mems)
        val obj = JSONObject(jsonStr)
        assertEquals(2, obj.getJSONArray("messages").length())
        assertEquals(1, obj.getJSONArray("memories").length())
    }
}
