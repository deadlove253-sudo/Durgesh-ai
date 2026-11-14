package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [ChatMessageEntity::class, VoiceCommandHistoryEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class DurgeshDatabase : RoomDatabase() {
  abstract fun assistantDao(): AssistantDao

  companion object {
    @Volatile private var instance: DurgeshDatabase? = null

    fun getInstance(context: Context): DurgeshDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                DurgeshDatabase::class.java,
                "durgesh_ai.db",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
