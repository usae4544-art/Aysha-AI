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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val source: String = "aapne bataya",
    val archived: Boolean = false,
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
    val playedCount: Int = 0,
)

@Entity(tableName = "tool_logs")
data class ToolLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val summary: String,
    val success: Boolean,
    val timestampMs: Long,
)

@Entity(
    tableName = "user_rules",
    indices = [Index(value = ["text"], unique = true)],
)
data class UserRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val enabled: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "knowledge_documents",
    indices = [Index(value = ["title"], unique = true)],
)
data class KnowledgeDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val chunkCount: Int = 1,
    val addedAtMs: Long = System.currentTimeMillis(),
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

    @Query("UPDATE memory_entries SET archived = :archived, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAtMs: Long = System.currentTimeMillis())

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

    @Query("UPDATE macro_definitions SET playedCount = playedCount + 1, updatedAtMs = :ts WHERE name = :name")
    suspend fun incrementPlayed(name: String, ts: Long = System.currentTimeMillis())

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

@Dao
interface UserRuleDao {
    @Query("SELECT * FROM user_rules ORDER BY createdAtMs DESC")
    suspend fun getAll(): List<UserRuleEntity>

    @Query("SELECT * FROM user_rules WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): UserRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserRuleEntity): Long

    @Query("DELETE FROM user_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_rules")
    suspend fun clearAll()
}

@Dao
interface KnowledgeDocumentDao {
    @Query("SELECT * FROM knowledge_documents ORDER BY addedAtMs DESC")
    suspend fun getAll(): List<KnowledgeDocumentEntity>

    @Query("SELECT * FROM knowledge_documents WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): KnowledgeDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KnowledgeDocumentEntity): Long

    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM knowledge_documents")
    suspend fun clearAll()
}

@Database(
    entities = [
        ChatMessageEntity::class,
        MemoryEntryEntity::class,
        MacroEntity::class,
        ToolLogEntity::class,
        UserRuleEntity::class,
        KnowledgeDocumentEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AyshaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun macroDao(): MacroDao
    abstract fun toolLogDao(): ToolLogDao
    abstract fun ruleDao(): UserRuleDao
    abstract fun documentDao(): KnowledgeDocumentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN source TEXT NOT NULL DEFAULT 'aapne bataya'")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE macro_definitions ADD COLUMN playedCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_rules_text ON user_rules(text)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS knowledge_documents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        chunkCount INTEGER NOT NULL,
                        addedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_knowledge_documents_title ON knowledge_documents(title)")
            }
        }
    }
}
