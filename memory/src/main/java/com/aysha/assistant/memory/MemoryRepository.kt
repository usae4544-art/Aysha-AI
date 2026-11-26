package com.aysha.assistant.memory

import android.content.Context
import androidx.room.Room
import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.MacroDefinition
import com.aysha.assistant.core.MacroStep
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.SafeLogger
import com.aysha.assistant.core.ToolLogEntry
import org.json.JSONArray
import org.json.JSONObject

class MemoryRepository(private val context: Context) {

    private val db: AyshaDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AyshaDatabase::class.java,
            "aysha_vault.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    suspend fun ensureBuiltInMacrosSeeded() {
        val existing = db.macroDao().getAll()
        if (existing.isNotEmpty()) return
        for (macro in BuiltInMacros.defaults()) {
            upsertMacro(macro)
        }
    }

    suspend fun addMessage(message: ChatMessage): Long {
        return db.chatDao().insert(
            ChatMessageEntity(
                role = message.role.name,
                text = message.text,
                providerTag = message.providerTag,
                imagePath = message.imagePath,
                timestampMs = message.timestampMs,
            ),
        )
    }

    suspend fun getRecentMessages(limit: Int = 200): List<ChatMessage> {
        return db.chatDao().getRecentMessagesDesc(limit).reversed().map { it.toDomain() }
    }

    suspend fun searchMessages(query: String): List<ChatMessage> {
        if (query.isBlank()) return getRecentMessages()
        return db.chatDao().searchMessages(query.trim()).map { it.toDomain() }
    }

    suspend fun deleteMessage(id: Long) {
        db.chatDao().deleteById(id)
    }

    suspend fun clearMessages() {
        db.chatDao().clearAll()
    }

    suspend fun saveMemory(
        title: String,
        content: String,
        tags: String = "",
        approvedByUser: Boolean = true,
    ): Long {
        val cleanTitle = title.trim().ifBlank { "Note" }
        val existing = db.memoryDao().findByTitle(cleanTitle)
        return db.memoryDao().upsert(
            MemoryEntryEntity(
                id = existing?.id ?: 0,
                title = cleanTitle,
                content = content.trim(),
                tags = tags.trim(),
                approvedByUser = approvedByUser,
                updatedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateMemory(entry: MemoryEntry) {
        db.memoryDao().upsert(
            MemoryEntryEntity(
                id = entry.id,
                title = entry.title.trim(),
                content = entry.content.trim(),
                tags = entry.tags.trim(),
                approvedByUser = entry.approvedByUser,
                updatedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getAllMemories(): List<MemoryEntry> {
        return db.memoryDao().getAll().map { it.toDomain() }
    }

    suspend fun searchMemories(query: String, limit: Int = 8): List<MemoryEntry> {
        return MemoryMatcher.rank(getAllMemories(), query, limit)
    }

    suspend fun deleteMemory(id: Long) {
        db.memoryDao().deleteById(id)
    }

    suspend fun clearMemories() {
        db.memoryDao().clearAll()
    }

    suspend fun getAllMacros(): List<MacroDefinition> {
        ensureBuiltInMacrosSeeded()
        return db.macroDao().getAll().map { it.toDomain() }
    }

    suspend fun getMacroByNameOrTrigger(query: String): MacroDefinition? {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return null
        val all = getAllMacros()
        return all.firstOrNull { macro ->
            macro.name.equals(clean, ignoreCase = true) ||
                (macro.triggerPhrase.isNotBlank() && clean.contains(macro.triggerPhrase.lowercase()))
        }
    }

    suspend fun upsertMacro(macro: MacroDefinition) {
        db.macroDao().upsert(macro.toEntity())
    }

    suspend fun deleteMacro(name: String) {
        db.macroDao().deleteByName(name)
    }

    suspend fun logTool(toolName: String, summary: String, success: Boolean) {
        db.toolLogDao().insert(
            ToolLogEntity(
                toolName = toolName,
                summary = SafeLogger.redact(summary).take(500),
                success = success,
                timestampMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getRecentToolLogs(limit: Int = 50): List<ToolLogEntry> {
        return db.toolLogDao().getRecent(limit).map {
            ToolLogEntry(
                id = it.id,
                toolName = it.toolName,
                summary = it.summary,
                success = it.success,
                timestampMs = it.timestampMs,
            )
        }
    }

    suspend fun clearAllLocalData() {
        db.chatDao().clearAll()
        db.memoryDao().clearAll()
        db.toolLogDao().clearAll()
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.ASSISTANT),
        text = text,
        providerTag = providerTag,
        imagePath = imagePath,
        timestampMs = timestampMs,
    )

    private fun MemoryEntryEntity.toDomain(): MemoryEntry = MemoryEntry(
        id = id,
        title = title,
        content = content,
        tags = tags,
        approvedByUser = approvedByUser,
        updatedAtMs = updatedAtMs,
    )

    private fun MacroEntity.toDomain(): MacroDefinition {
        val paramsArr = runCatching { JSONArray(paramsJson) }.getOrNull() ?: JSONArray()
        val params = (0 until paramsArr.length()).mapNotNull { paramsArr.optString(it).takeIf(String::isNotBlank) }

        val stepsArr = runCatching { JSONArray(stepsJson) }.getOrNull() ?: JSONArray()
        val steps = (0 until stepsArr.length()).mapNotNull { idx ->
            val obj = stepsArr.optJSONObject(idx) ?: return@mapNotNull null
            val tool = obj.optString("tool")
            if (tool.isBlank()) return@mapNotNull null
            val argsObj = obj.optJSONObject("args") ?: JSONObject()
            val args = mutableMapOf<String, String>()
            val keys = argsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k != "_optional") {
                    args[k] = argsObj.opt(k)?.toString().orEmpty()
                }
            }
            val optional = obj.optBoolean("optional", argsObj.optBoolean("_optional", false))
            MacroStep(tool = tool, args = args, optional = optional)
        }

        return MacroDefinition(
            name = name,
            triggerPhrase = triggerPhrase,
            intent = intent,
            params = params,
            steps = steps,
            isBuiltIn = isBuiltIn,
        )
    }

    private fun MacroDefinition.toEntity(): MacroEntity {
        val paramsArr = JSONArray().apply { params.forEach { put(it) } }
        val stepsArr = JSONArray().apply {
            steps.forEach { s ->
                put(
                    JSONObject().apply {
                        put("tool", s.tool)
                        put("optional", s.optional)
                        put("args", JSONObject().apply { s.args.forEach { (k, v) -> put(k, v) } })
                    },
                )
            }
        }
        return MacroEntity(
            name = name,
            triggerPhrase = triggerPhrase,
            intent = intent,
            paramsJson = paramsArr.toString(),
            stepsJson = stepsArr.toString(),
            isBuiltIn = isBuiltIn,
            updatedAtMs = System.currentTimeMillis(),
        )
    }
}

object BuiltInMacros {
    fun defaults(): List<MacroDefinition> = listOf(
        MacroDefinition(
            name = "Open YouTube",
            triggerPhrase = "open youtube",
            intent = "Launch YouTube app quickly",
            steps = listOf(
                MacroStep(tool = "open_app", args = mapOf("name" to "YouTube")),
            ),
            isBuiltIn = true,
        ),
        MacroDefinition(
            name = "Study Mode",
            triggerPhrase = "start study",
            intent = "Open study app, set focus timer for 25 minutes & adjust volume",
            steps = listOf(
                MacroStep(tool = "set_volume", args = mapOf("stream" to "ring", "percent" to "10"), optional = true),
                MacroStep(tool = "start_timer", args = mapOf("seconds" to "1500", "label" to "Study Focus")),
                MacroStep(tool = "open_app", args = mapOf("name" to "YouTube"), optional = true),
            ),
            isBuiltIn = true,
        ),
        MacroDefinition(
            name = "Play Music",
            triggerPhrase = "play music",
            intent = "Resume or toggle media playback",
            steps = listOf(
                MacroStep(tool = "media_control", args = mapOf("action" to "play_pause")),
            ),
            isBuiltIn = true,
        ),
        MacroDefinition(
            name = "Turn on Flashlight",
            triggerPhrase = "turn on flash",
            intent = "Switch on the rear flashlight torch",
            steps = listOf(
                MacroStep(tool = "set_torch", args = mapOf("enabled" to "true")),
            ),
            isBuiltIn = true,
        ),
    )
}
