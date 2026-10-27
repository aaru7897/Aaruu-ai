package com.example.data.repository

import com.example.data.local.AaruDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import kotlinx.coroutines.flow.Flow

class AaruRepository(private val dao: AaruDao) {

    // Memories
    val allMemoriesFlow: Flow<List<MemoryEntity>> = dao.getAllMemoriesFlow()

    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> {
        return dao.getMemoriesByCategoryFlow(category)
    }

    fun searchMemories(query: String): Flow<List<MemoryEntity>> {
        return dao.searchMemoriesFlow(query)
    }

    suspend fun getAllMemoriesList(): List<MemoryEntity> {
        return dao.getAllMemoriesList()
    }

    suspend fun saveMemory(
        key: String,
        content: String,
        category: String = "PERSONAL",
        importance: Int = 3
    ): Long {
        val memory = MemoryEntity(
            key = key.trim(),
            content = content.trim(),
            category = category.trim().uppercase(),
            importance = importance.coerceIn(1, 5)
        )
        return dao.insertMemory(memory)
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        dao.updateMemory(memory)
    }

    suspend fun deleteMemory(id: Long) {
        dao.deleteMemoryById(id)
    }

    suspend fun clearAllMemories() {
        dao.clearAllMemories()
    }

    suspend fun getMemoryCount(): Int {
        return dao.getMemoryCount()
    }

    suspend fun touchMemoryRecall(id: Long) {
        dao.updateLastRecalled(id)
    }

    // Conversations
    val allMessagesFlow: Flow<List<ConversationEntity>> = dao.getAllMessagesFlow()

    suspend fun addMessage(
        role: String,
        content: String,
        actionType: String? = null,
        actionPayload: String? = null
    ): Long {
        val message = ConversationEntity(
            role = role,
            content = content,
            actionType = actionType,
            actionPayload = actionPayload,
            timestamp = System.currentTimeMillis()
        )
        return dao.insertMessage(message)
    }

    suspend fun getRecentMessages(limit: Int = 15): List<ConversationEntity> {
        return dao.getRecentMessagesList(limit)
    }

    suspend fun clearChat() {
        dao.clearAllConversations()
    }
}
