package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MemoryEntity::class, ConversationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AaruDatabase : RoomDatabase() {

    abstract fun aaruDao(): AaruDao

    companion object {
        @Volatile
        private var INSTANCE: AaruDatabase? = null

        fun getDatabase(context: Context): AaruDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AaruDatabase::class.java,
                    "aaru_assistant_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    val dao = database.aaruDao()
                                    val seedMemories = listOf(
                                        MemoryEntity(
                                            key = "user_nickname",
                                            content = "User's nickname is Boss (Nishad). Always address him warmly as Boss.",
                                            category = "PERSONAL",
                                            importance = 5
                                        ),
                                        MemoryEntity(
                                            key = "assistant_identity",
                                            content = "Your name is Aaru AI, Boss's personal JARVIS-inspired emotionally intelligent voice companion.",
                                            category = "PERSONAL",
                                            importance = 5
                                        ),
                                        MemoryEntity(
                                            key = "preferred_language",
                                            content = "Primary language is Hindi & Hinglish, with natural English support. Speak like a close friend, never robotic.",
                                            category = "PREFERENCE",
                                            importance = 5
                                        ),
                                        MemoryEntity(
                                            key = "preferred_apps",
                                            content = "Frequently uses YouTube, WhatsApp, Instagram, Google Search, Alarms, and Timers.",
                                            category = "ROUTINE",
                                            importance = 4
                                        ),
                                        MemoryEntity(
                                            key = "dev_workflow",
                                            content = "Boss builds projects directly from his Android smartphone with practical, high-performance tools.",
                                            category = "WORK",
                                            importance = 4
                                        )
                                    )
                                    dao.insertMemories(seedMemories)

                                    dao.insertMessage(
                                        ConversationEntity(
                                            role = "aaru",
                                            content = "Haan Boss, bolo ❤️ Main Aaru AI ready hoon! Aap mujhse Hindi, Hinglish ya English mein baat kar sakte ho—chaho toh bolo 'Aaru, suno!' ya neeche mic dabao."
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
