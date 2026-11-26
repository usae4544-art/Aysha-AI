package com.aysha.assistant.memory

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val providerTag: String,
    val imagePath: String?,
    val timestampMs: Long,
)

@Entity(
    tableName = "memory_entries",
    indices = [Index(value = ["title"], unique = true)],
)
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String,
    val approvedByUser: Boolean,
    val updatedAtMs: Long,
)

@Entity(tableName = "macro_definitions")
data class MacroEntity(
    @PrimaryKey val name: String,
    val triggerPhrase: String,
    val intent: String,
    val paramsJson: String,
    val stepsJson: String,
    val isBuiltIn: Boolean,
    val updatedAtMs: Long,
)

@Entity(tableName = "tool_logs")
data class ToolLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val summary: String,
    val success: Boolean,
    val timestampMs: Long,
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentMessagesDesc(limit: Int = 200): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY id DESC")
    fun observeAllDesc(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' OR providerTag LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    suspend fun searchMessages(query: String, limit: Int = 80): List<ChatMessageEntity>

    @Insert
    suspend fun insert(entity: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries ORDER BY updatedAtMs DESC")
    suspend fun getAll(): List<MemoryEntryEntity>

    @Query("SELECT * FROM memory_entries ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): MemoryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoryEntryEntity): Long

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAll()
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM macro_definitions ORDER BY isBuiltIn DESC, name ASC")
    suspend fun getAll(): List<MacroEntity>

    @Query("SELECT * FROM macro_definitions WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MacroEntity)

    @Query("DELETE FROM macro_definitions WHERE name = :name")
    suspend fun deleteByName(name: String)
}

@Dao
interface ToolLogDao {
    @Query("SELECT * FROM tool_logs ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<ToolLogEntity>

    @Insert
    suspend fun insert(entity: ToolLogEntity): Long

    @Query("DELETE FROM tool_logs")
    suspend fun clearAll()
}

@Database(
    entities = [
        ChatMessageEntity::class,
        MemoryEntryEntity::class,
        MacroEntity::class,
        ToolLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AyshaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun macroDao(): MacroDao
    abstract fun toolLogDao(): ToolLogDao
}
