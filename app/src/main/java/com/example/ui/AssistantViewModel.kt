package com.example.ui

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AssistantPreferences
import com.example.data.local.AssistantRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommandExecutionStatus
import com.example.data.local.DurgeshDatabase
import com.example.data.local.InputMode
import com.example.data.local.MessageStatus
import com.example.data.local.VoiceCommandHistoryEntity
import com.example.domain.AppTarget
import com.example.domain.CommandExecutionResult
import com.example.domain.HindiCommandProcessor
import com.example.domain.ListeningState
import com.example.domain.SettingsTarget
import com.example.system.DeviceController
import com.example.system.DeviceStatusSnapshot
import com.example.system.HindiVoiceEngine
import com.example.system.VoiceEngineCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantTab {
  ASSISTANT,
  COMMANDS,
  SETTINGS,
}

data class PermissionDialogState(
  val permission: String,
  val titleHindi: String,
  val titleEnglish: String,
  val explanationHindi: String,
  val explanationEnglish: String,
  val isPermanentlyDenied: Boolean = false,
  val pendingCommandAfterGrant: String? = null,
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: AssistantRepository
  private val commandProcessor = HindiCommandProcessor()
  private val deviceController = DeviceController(application.applicationContext)
  private val voiceEngine: HindiVoiceEngine

  val chatMessages: StateFlow<List<ChatMessageEntity>>
  val voiceHistory: StateFlow<List<VoiceCommandHistoryEntity>>
  val preferences: StateFlow<AssistantPreferences>
  val listeningState: StateFlow<ListeningState>
  val capabilities: StateFlow<VoiceEngineCapabilities>

  private val _deviceStatus = MutableStateFlow(deviceController.getDeviceStatusSnapshot())
  val deviceStatus: StateFlow<DeviceStatusSnapshot> = _deviceStatus.asStateFlow()

  private val _micGranted = MutableStateFlow(deviceController.isMicrophoneGranted())
  val micGranted: StateFlow<Boolean> = _micGranted.asStateFlow()

  private val _cameraGranted = MutableStateFlow(deviceController.isCameraGranted())
  val cameraGranted: StateFlow<Boolean> = _cameraGranted.asStateFlow()

  private val _permissionDialog = MutableStateFlow<PermissionDialogState?>(null)
  val permissionDialog: StateFlow<PermissionDialogState?> = _permissionDialog.asStateFlow()

  private val _commandFilter = MutableStateFlow<String>("ALL")
  val commandFilter: StateFlow<String> = _commandFilter.asStateFlow()

  private var startVoiceAfterMicGrant = false

  init {
    val db = DurgeshDatabase.getInstance(application.applicationContext)
    repository = AssistantRepository(db.assistantDao(), application.applicationContext)

    chatMessages =
      repository.chatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

    voiceHistory =
      repository.voiceHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

    preferences =
      repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AssistantPreferences(),
      )

    voiceEngine =
      HindiVoiceEngine(
        context = application.applicationContext,
        onSpeechRecognized = { recognizedText ->
          processCommand(recognizedText, InputMode.VOICE)
        },
        onRecognitionError = { hindiMsg, englishMsg ->
          viewModelScope.launch {
            repository.saveChatMessage(
              ChatMessageEntity(
                text = hindiMsg,
                subtitle = englishMsg,
                isFromUser = false,
                inputMode = InputMode.SYSTEM.name,
                status = MessageStatus.CANNOT_PERFORM.name,
              )
            )
            repository.saveVoiceCommandHistory(
              VoiceCommandHistoryEntity(
                spokenText = "(आवाज़ स्पष्ट नहीं थी)",
                category = "वॉइस रिकग्निशन",
                executionStatus = CommandExecutionStatus.FAILED.name,
                detailMessage = hindiMsg,
                isOfflineAttempt = preferences.value.preferOfflineSpeech,
              )
            )
            if (preferences.value.autoSpeakHindi) {
              voiceEngine.speak(hindiMsg, preferences.value.speechRate)
            }
          }
        },
      )

    listeningState = voiceEngine.listeningState
    capabilities = voiceEngine.capabilities
  }

  fun refreshSystemStatus() {
    _deviceStatus.value = deviceController.getDeviceStatusSnapshot()
    _micGranted.value = deviceController.isMicrophoneGranted()
    _cameraGranted.value = deviceController.isCameraGranted()
  }

  fun onMicButtonTapped() {
    refreshSystemStatus()
    when (listeningState.value) {
      is ListeningState.Listening -> {
        voiceEngine.stopListening()
      }
      is ListeningState.Speaking -> {
        voiceEngine.stopSpeaking()
      }
      is ListeningState.Processing -> Unit
      is ListeningState.Idle -> {
        if (!deviceController.isMicrophoneGranted()) {
          startVoiceAfterMicGrant = true
          _permissionDialog.value =
            PermissionDialogState(
              permission = Manifest.permission.RECORD_AUDIO,
              titleHindi = "माइक्रोफ़ोन अनुमति आवश्यक है",
              titleEnglish = "Microphone Permission Needed",
              explanationHindi =
                "आपकी हिंदी आवाज़ सुनने के लिए Durgesh AI को माइक्रोफ़ोन की अनुमति चाहिए। यह केवल आपके माइक बटन दबाने पर ही सुनता है — बैकग्राउंड में कभी रिकॉर्डिंग नहीं होती।",
              explanationEnglish =
                "Durgesh AI only listens when you explicitly tap the microphone button. No hidden background recording.",
            )
        } else {
          voiceEngine.startListening(preferences.value.preferOfflineSpeech)
        }
      }
    }
  }

  fun requestPermissionPrompt(permission: String, isPermanentlyDenied: Boolean = false) {
    _permissionDialog.value =
      when (permission) {
        Manifest.permission.RECORD_AUDIO ->
          PermissionDialogState(
            permission = permission,
            titleHindi = "माइक्रोफ़ोन अनुमति",
            titleEnglish = "Microphone Permission",
            explanationHindi =
              if (isPermanentlyDenied) {
                "माइक्रोफ़ोन अनुमति बंद है। कृपया App Settings में जाकर Microphone अनुमति चालू करें।"
              } else {
                "हिंदी वॉइस कमांड सुनने के लिए माइक्रोफ़ोन अनुमति दें। रिकॉर्डिंग सिर्फ माइक दबाने पर चालू होती है।"
              },
            explanationEnglish =
              if (isPermanentlyDenied) {
                "Microphone permission was denied. Enable it in App Settings to use voice commands."
              } else {
                "Allow Microphone access to speak Hindi commands into Android SpeechRecognizer."
              },
            isPermanentlyDenied = isPermanentlyDenied,
          )
        Manifest.permission.CAMERA ->
          PermissionDialogState(
            permission = permission,
            titleHindi = "फ़्लैशलाइट / कैमरा अनुमति",
            titleEnglish = "Camera Permission for Flashlight",
            explanationHindi =
              if (isPermanentlyDenied) {
                "फ़्लैशलाइट नियंत्रित करने के लिए App Settings से Camera अनुमति चालू करें।"
              } else {
                "डिवाइस की टॉर्च / फ़्लैशलाइट चालू या बंद करने के लिए कैमरा हार्डवेयर अनुमति आवश्यक है।"
              },
            explanationEnglish =
              if (isPermanentlyDenied) {
                "Enable Camera permission in App Settings to control the hardware torch."
              } else {
                "Allow Camera hardware access so Durgesh AI can toggle your phone flashlight."
              },
            isPermanentlyDenied = isPermanentlyDenied,
          )
        else -> null
      }
  }

  fun dismissPermissionDialog() {
    startVoiceAfterMicGrant = false
    _permissionDialog.value = null
  }

  fun onPermissionResult(permission: String, granted: Boolean, shouldShowRationale: Boolean) {
    val pendingCmd = _permissionDialog.value?.pendingCommandAfterGrant
    refreshSystemStatus()

    if (granted) {
      _permissionDialog.value = null
      if (permission == Manifest.permission.RECORD_AUDIO && startVoiceAfterMicGrant) {
        startVoiceAfterMicGrant = false
        voiceEngine.startListening(preferences.value.preferOfflineSpeech)
      } else if (permission == Manifest.permission.CAMERA && !pendingCmd.isNullOrBlank()) {
        processCommand(pendingCmd, InputMode.TEXT)
      }
    } else {
      startVoiceAfterMicGrant = false
      val permanentlyDenied = !shouldShowRationale
      _permissionDialog.value =
        PermissionDialogState(
          permission = permission,
          titleHindi =
            if (permission == Manifest.permission.RECORD_AUDIO) {
              "माइक्रोफ़ोन अनुमति नहीं मिली"
            } else {
              "कैमरा अनुमति नहीं मिली"
            },
          titleEnglish =
            if (permission == Manifest.permission.RECORD_AUDIO) {
              "Microphone Permission Denied"
            } else {
              "Camera Permission Denied"
            },
          explanationHindi =
            if (permanentlyDenied) {
              "अनुमति स्थायी रूप से बंद है। कृपया नीचे दिए गए बटन से App Settings खोलकर अनुमति दें।"
            } else {
              "बिना अनुमति के यह फ़ोन फ़ीचर उपयोग नहीं किया जा सकता। आप नीचे टाइप करके भी कमांड चला सकते हैं।"
            },
          explanationEnglish =
            if (permanentlyDenied) {
              "Permission is blocked. Open App Settings to enable it."
            } else {
              "This phone feature requires permission before access."
            },
          isPermanentlyDenied = permanentlyDenied,
        )

      viewModelScope.launch {
        val hindiDenyMsg =
          if (permission == Manifest.permission.RECORD_AUDIO) {
            "माइक्रोफ़ोन अनुमति नहीं दी गई, इसलिए वॉइस रिकॉर्डिंग शुरू नहीं की जा सकी।"
          } else {
            "कैमरा अनुमति नहीं दी गई, इसलिए फ़्लैशलाइट नियंत्रित नहीं की जा सकी।"
          }
        repository.saveChatMessage(
          ChatMessageEntity(
            text = hindiDenyMsg,
            subtitle = "Permission required before accessing phone hardware.",
            isFromUser = false,
            inputMode = InputMode.SYSTEM.name,
            status = MessageStatus.PERMISSION_REQUIRED.name,
          )
        )
      }
    }
  }

  fun openAppSettings() {
    deviceController.openAppPermissionSettings()
    dismissPermissionDialog()
  }

  fun submitTypedCommand(text: String) {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return
    processCommand(trimmed, InputMode.TEXT)
  }

  fun runQuickCommand(text: String) {
    processCommand(text, InputMode.TEXT)
  }

  fun launchAppDirect(target: AppTarget) {
    val label = "${target.hindiName} खोलो"
    processCommand(label, InputMode.TEXT)
  }

  fun openSettingsDirect(target: SettingsTarget) {
    val label = "${target.hindiName} खोलो"
    processCommand(label, InputMode.TEXT)
  }

  private fun processCommand(rawText: String, inputMode: InputMode) {
    refreshSystemStatus()
    val prefs = preferences.value

    viewModelScope.launch {
      // 1. Persist user message
      repository.saveChatMessage(
        ChatMessageEntity(
          text = rawText,
          isFromUser = true,
          inputMode = inputMode.name,
          status = MessageStatus.INFO.name,
        )
      )

      // 2. Parse and execute real Android intent/command
      val parsedIntent = commandProcessor.parse(rawText)
      val result = deviceController.execute(parsedIntent, rawText)

      // 3. Persist AI reply & command history
      when (result) {
        is CommandExecutionResult.Success -> {
          repository.saveChatMessage(
            ChatMessageEntity(
              text = result.hindiReply,
              subtitle = result.englishNote,
              isFromUser = false,
              inputMode = InputMode.SYSTEM.name,
              status = MessageStatus.SUCCESS.name,
              actionLabel = result.actionLabel,
            )
          )
          if (inputMode == InputMode.VOICE || true) {
            repository.saveVoiceCommandHistory(
              VoiceCommandHistoryEntity(
                spokenText = rawText,
                category = result.category,
                executionStatus = CommandExecutionStatus.SUCCESS.name,
                detailMessage = result.hindiReply,
                isOfflineAttempt = prefs.preferOfflineSpeech,
              )
            )
          }
          if (prefs.autoSpeakHindi) {
            voiceEngine.speak(result.hindiReply, prefs.speechRate)
          }
        }
        is CommandExecutionResult.CannotPerform -> {
          repository.saveChatMessage(
            ChatMessageEntity(
              text = result.hindiReason,
              subtitle = result.englishReason,
              isFromUser = false,
              inputMode = InputMode.SYSTEM.name,
              status = MessageStatus.CANNOT_PERFORM.name,
            )
          )
          repository.saveVoiceCommandHistory(
            VoiceCommandHistoryEntity(
              spokenText = rawText,
              category = result.category,
              executionStatus = CommandExecutionStatus.FAILED.name,
              detailMessage = result.hindiReason,
              isOfflineAttempt = prefs.preferOfflineSpeech,
            )
          )
          if (prefs.autoSpeakHindi) {
            voiceEngine.speak(result.hindiReason, prefs.speechRate)
          }
        }
        is CommandExecutionResult.PermissionRequired -> {
          repository.saveChatMessage(
            ChatMessageEntity(
              text = result.hindiReason,
              subtitle = result.englishReason,
              isFromUser = false,
              inputMode = InputMode.SYSTEM.name,
              status = MessageStatus.PERMISSION_REQUIRED.name,
            )
          )
          repository.saveVoiceCommandHistory(
            VoiceCommandHistoryEntity(
              spokenText = rawText,
              category = result.category,
              executionStatus = CommandExecutionStatus.PERMISSION_DENIED.name,
              detailMessage = result.hindiReason,
              isOfflineAttempt = prefs.preferOfflineSpeech,
            )
          )
          _permissionDialog.value =
            PermissionDialogState(
              permission = result.permission,
              titleHindi = "फ़ोन फ़ीचर अनुमति आवश्यक",
              titleEnglish = "Permission Required",
              explanationHindi = result.hindiReason,
              explanationEnglish = result.englishReason,
              pendingCommandAfterGrant = result.pendingCommandText,
            )
          if (prefs.autoSpeakHindi) {
            voiceEngine.speak(result.hindiReason, prefs.speechRate)
          }
        }
      }
    }
  }

  fun speakMessage(text: String) {
    voiceEngine.speak(text, preferences.value.speechRate)
  }

  fun stopSpeaking() {
    voiceEngine.stopSpeaking()
  }

  fun setCommandFilter(filter: String) {
    _commandFilter.value = filter
  }

  fun deleteVoiceHistoryItem(id: Long) {
    viewModelScope.launch { repository.deleteVoiceCommand(id) }
  }

  fun clearVoiceHistory() {
    viewModelScope.launch { repository.clearVoiceHistory() }
  }

  fun clearChatHistory() {
    viewModelScope.launch { repository.clearChatHistory() }
  }

  fun setAutoSpeakHindi(enabled: Boolean) {
    viewModelScope.launch { repository.setAutoSpeakHindi(enabled) }
  }

  fun setPreferOfflineSpeech(enabled: Boolean) {
    viewModelScope.launch { repository.setPreferOfflineSpeech(enabled) }
  }

  fun setSpeechRate(rate: Float) {
    viewModelScope.launch { repository.setSpeechRate(rate) }
  }

  override fun onCleared() {
    super.onCleared()
    voiceEngine.release()
  }
}
