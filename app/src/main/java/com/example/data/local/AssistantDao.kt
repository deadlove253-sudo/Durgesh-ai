package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
  @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
  fun observeChatMessages(): Flow<List<ChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChatMessage(message: ChatMessageEntity): Long

  @Query("DELETE FROM chat_messages")
  suspend fun clearChatMessages()

  @Query("SELECT * FROM voice_command_history ORDER BY timestamp DESC, id DESC")
  fun observeVoiceHistory(): Flow<List<VoiceCommandHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVoiceHistory(entry: VoiceCommandHistoryEntity): Long

  @Query("DELETE FROM voice_command_history WHERE id = :id")
  suspend fun deleteVoiceHistoryById(id: Long)

  @Query("DELETE FROM voice_command_history")
  suspend fun clearVoiceHistory()
}
