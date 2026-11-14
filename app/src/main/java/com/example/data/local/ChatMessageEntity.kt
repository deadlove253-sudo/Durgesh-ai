package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus {
  SUCCESS,
  CANNOT_PERFORM,
  PERMISSION_REQUIRED,
  INFO,
}

enum class InputMode {
  VOICE,
  TEXT,
  SYSTEM,
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val text: String,
  val subtitle: String? = null,
  val isFromUser: Boolean,
  val inputMode: String = InputMode.VOICE.name,
  val status: String = MessageStatus.INFO.name,
  val actionLabel: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
)
