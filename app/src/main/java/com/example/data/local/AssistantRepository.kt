package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.assistantDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "durgesh_ai_prefs")

data class AssistantPreferences(
  val autoSpeakHindi: Boolean = true,
  val preferOfflineSpeech: Boolean = false,
  val speechRate: Float = 1.0f,
)

class AssistantRepository(private val dao: AssistantDao, private val context: Context) {
  private object Keys {
    val AUTO_SPEAK_HINDI = booleanPreferencesKey("auto_speak_hindi")
    val PREFER_OFFLINE_SPEECH = booleanPreferencesKey("prefer_offline_speech")
    val SPEECH_RATE = floatPreferencesKey("speech_rate")
  }

  val chatMessages: Flow<List<ChatMessageEntity>> = dao.observeChatMessages()

  val voiceHistory: Flow<List<VoiceCommandHistoryEntity>> = dao.observeVoiceHistory()

  val preferences: Flow<AssistantPreferences> =
    context.assistantDataStore.data.map { prefs ->
      AssistantPreferences(
        autoSpeakHindi = prefs[Keys.AUTO_SPEAK_HINDI] ?: true,
        preferOfflineSpeech = prefs[Keys.PREFER_OFFLINE_SPEECH] ?: false,
        speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
      )
    }

  suspend fun saveChatMessage(message: ChatMessageEntity) {
    dao.insertChatMessage(message)
  }

  suspend fun clearChatHistory() {
    dao.clearChatMessages()
  }

  suspend fun saveVoiceCommandHistory(entry: VoiceCommandHistoryEntity) {
    dao.insertVoiceHistory(entry)
  }

  suspend fun deleteVoiceCommand(id: Long) {
    dao.deleteVoiceHistoryById(id)
  }

  suspend fun clearVoiceHistory() {
    dao.clearVoiceHistory()
  }

  suspend fun setAutoSpeakHindi(enabled: Boolean) {
    context.assistantDataStore.edit { it[Keys.AUTO_SPEAK_HINDI] = enabled }
  }

  suspend fun setPreferOfflineSpeech(enabled: Boolean) {
    context.assistantDataStore.edit { it[Keys.PREFER_OFFLINE_SPEECH] = enabled }
  }

  suspend fun setSpeechRate(rate: Float) {
    context.assistantDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.5f) }
  }
}
