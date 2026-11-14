package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommandExecutionStatus {
  SUCCESS,
  FAILED,
  PERMISSION_DENIED,
}

@Entity(tableName = "voice_command_history")
data class VoiceCommandHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val spokenText: String,
  val category: String,
  val executionStatus: String,
  val detailMessage: String,
  val isOfflineAttempt: Boolean,
  val timestamp: Long = System.currentTimeMillis(),
)
