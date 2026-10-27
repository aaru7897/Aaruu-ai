package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "aaru", "system", "action"
    val content: String,
    val actionType: String? = null, // "WHATSAPP", "OPEN_APP", "WEB_SEARCH", "CALL", "ALARM", "NONE"
    val actionPayload: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
