package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val content: String,
    val category: String, // PERSONAL, PREFERENCE, PEOPLE, WORK, ROUTINE, GENERAL
    val importance: Int = 3, // 1 to 5
    val createdAt: Long = System.currentTimeMillis(),
    val lastRecalledAt: Long = System.currentTimeMillis()
)
