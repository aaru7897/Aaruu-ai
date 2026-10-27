package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AaruDao {

    // Memories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM memories ORDER BY importance DESC, lastRecalledAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY importance DESC, lastRecalledAt DESC")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC, lastRecalledAt DESC")
    fun getMemoriesByCategoryFlow(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' OR `key` LIKE '%' || :query || '%'")
    fun searchMemoriesFlow(query: String): Flow<List<MemoryEntity>>

    @Query("UPDATE memories SET lastRecalledAt = :timestamp WHERE id = :id")
    suspend fun updateLastRecalled(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoryCount(): Int

    // Conversation History
    @Insert
    suspend fun insertMessage(message: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesList(limit: Int = 20): List<ConversationEntity>

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}
