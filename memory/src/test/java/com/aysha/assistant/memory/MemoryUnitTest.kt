package com.aysha.assistant.memory

import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.KnowledgeDocument
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.UserRule
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryUnitTest {

    @Test
    fun `memory matcher ranks exact, semantic hinglish synonyms and ignores archived or unapproved`() {
        val entries = listOf(
            MemoryEntry(1, "Music", "Enjoys indie acoustic music", "music", approvedByUser = true),
            MemoryEntry(2, "Study", "Prefers 25 min Pomodoro study timer", "study,focus", approvedByUser = true),
            MemoryEntry(3, "Hidden", "Study notes unapproved", "study", approvedByUser = false),
            MemoryEntry(4, "Archived Study", "Old study plan", "study", approvedByUser = true, archived = true),
        )
        val ranked = MemoryMatcher.rank(entries, "padhai pomodoro", limit = 5)
        assertEquals(1, ranked.size)
        assertEquals("Study", ranked.first().title)
    }

    @Test
    fun `document chunking and search returns matching snippets`() {
        val doc = KnowledgeDocument(
            id = 1,
            title = "Android Roadmap",
            content = "Learn Kotlin coroutines and Jetpack Compose.\n\nPractice Room database and Gradle modular architecture.",
        )
        val chunks = MemoryMatcher.chunkDocument(doc.content, maxChunkChars = 60)
        assertTrue(chunks.size >= 2)
        val hits = MemoryMatcher.searchDocuments(listOf(doc), "Jetpack Compose")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.first().contains("Android Roadmap"))
    }

    @Test
    fun `chat exporter and full backup restore work without exposing api keys`() {
        val msgs = listOf(
            ChatMessage(1, ChatRole.USER, "Hello Aysha", "", null, 1700000000000L),
            ChatMessage(2, ChatRole.ASSISTANT, "Hi! How can I help?", "Offline", null, 1700000005000L),
        )
        val mems = listOf(MemoryEntry(1, "City", "Jaipur", "location"))
        val rules = listOf(UserRule(1, "Keep replies concise"))

        val md = ChatExporter.toMarkdown(msgs)
        assertTrue(md.contains("AYSHA Conversation Export"))
        assertTrue(md.contains("Hello Aysha"))

        val jsonStr = ChatExporter.toJson(msgs, mems)
        val obj = JSONObject(jsonStr)
        assertEquals(2, obj.getJSONArray("messages").length())
        assertEquals(1, obj.getJSONArray("memories").length())

        val backup = BackupRestoreHelper.buildBackupJson(
            settingsJson = """{"assistantName":"AYSHA","orbStyle":"AYSHA_2047"}""",
            memories = mems,
            rules = rules,
            documents = emptyList(),
            macros = emptyList(),
        )
        assertFalse(backup.contains("AIza"))
        val parsed = BackupRestoreHelper.parseBackupJson(backup).getOrThrow()
        assertEquals(1, parsed.memories.size)
        assertEquals(1, parsed.rules.size)
        assertEquals("Keep replies concise", parsed.rules.first().text)
    }
}
