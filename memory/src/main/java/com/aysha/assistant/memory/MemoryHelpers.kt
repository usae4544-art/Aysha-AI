package com.aysha.assistant.memory

import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.MemoryEntry
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MemoryMatcher {

    fun rank(entries: List<MemoryEntry>, query: String, limit: Int = 8): List<MemoryEntry> {
        val approved = entries.filter { it.approvedByUser }
        val q = query.trim().lowercase()
        if (q.isEmpty()) return approved.take(limit)

        val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
        return approved
            .map { entry ->
                val title = entry.title.lowercase()
                val content = entry.content.lowercase()
                val tags = entry.tags.lowercase()
                var score = 0
                if (title.contains(q)) score += 12
                if (content.contains(q)) score += 8
                if (tags.contains(q)) score += 6
                for (tok in tokens) {
                    if (title.contains(tok)) score += 4
                    if (content.contains(tok)) score += 2
                    if (tags.contains(tok)) score += 2
                }
                entry to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}

object ChatExporter {

    fun toMarkdown(messages: List<ChatMessage>, assistantName: String = "AYSHA"): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# $assistantName Conversation Export")
            appendLine()
            for (msg in messages) {
                val speaker = when (msg.role) {
                    ChatRole.USER -> "You"
                    ChatRole.ASSISTANT -> assistantName
                    ChatRole.SYSTEM -> "System"
                }
                val time = fmt.format(Date(msg.timestampMs))
                val tag = if (msg.providerTag.isNotBlank()) " (${msg.providerTag})" else ""
                appendLine("**$speaker$tag** · $time")
                appendLine(msg.text)
                appendLine()
            }
        }.trim()
    }

    fun toJson(messages: List<ChatMessage>, memories: List<MemoryEntry>): String {
        val msgArr = JSONArray()
        for (m in messages) {
            msgArr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("role", m.role.name)
                    put("text", m.text)
                    put("providerTag", m.providerTag)
                    put("timestampMs", m.timestampMs)
                },
            )
        }
        val memArr = JSONArray()
        for (mem in memories) {
            memArr.put(
                JSONObject().apply {
                    put("id", mem.id)
                    put("title", mem.title)
                    put("content", mem.content)
                    put("tags", mem.tags)
                    put("approvedByUser", mem.approvedByUser)
                    put("updatedAtMs", mem.updatedAtMs)
                },
            )
        }
        return JSONObject().apply {
            put("app", "AYSHA – Your Personal AI Assistant")
            put("exportedAtMs", System.currentTimeMillis())
            put("messages", msgArr)
            put("memories", memArr)
        }.toString(2)
    }
}
