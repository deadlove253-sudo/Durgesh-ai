package com.example.domain

sealed interface ListeningState {
  data object Idle : ListeningState

  data class Listening(val rmsDb: Float = 0f, val partialText: String = "") : ListeningState

  data object Processing : ListeningState

  data class Speaking(val utteranceId: String = "") : ListeningState
}

enum class AppTarget(val hindiName: String, val englishName: String, val packages: List<String>) {
  YOUTUBE("यूट्यूब", "YouTube", listOf("com.google.android.youtube")),
  WHATSAPP("व्हाट्सएप", "WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b")),
  CHROME("क्रोम", "Chrome", listOf("com.android.chrome")),
  CAMERA("कैमरा", "Camera", emptyList()),
  CALCULATOR(
    "कैलकुलेटर",
    "Calculator",
    listOf("com.miui.calculator", "com.google.android.calculator"),
  ),
  MESSAGES("मैसेज", "Messages", emptyList()),
  DIALER("फ़ोन डायलर", "Phone Dialer", emptyList()),
  MAPS("मैप्स", "Google Maps", listOf("com.google.android.apps.maps")),
  ALARMS("अलार्म / घड़ी", "Clock & Alarms", listOf("com.android.deskclock")),
  PLAY_STORE("प्ले स्टोर", "Play Store", listOf("com.android.vending")),
  GALLERY("गैलरी", "Gallery", emptyList()),
}

enum class SettingsTarget(val hindiName: String, val englishName: String) {
  WIFI("वाई-फ़ाई सेटिंग्स", "Wi-Fi Settings"),
  BLUETOOTH("ब्लूटूथ सेटिंग्स", "Bluetooth Settings"),
  PHONE_SETTINGS("फ़ोन सेटिंग्स", "Phone Settings"),
  INTERNET_PANEL("इंटरनेट पैनल", "Internet Panel"),
  VOLUME("साउंड सेटिंग्स", "Sound Settings"),
  DISPLAY("डिस्प्ले सेटिंग्स", "Display Settings"),
  BATTERY("बैटरी सेटिंग्स", "Battery Settings"),
  VOICE_INPUT("वॉइस इनपुट सेटिंग्स", "Voice Input Settings"),
}

sealed interface CommandIntent {
  data class OpenApp(val target: AppTarget) : CommandIntent

  data class OpenSettings(
    val target: SettingsTarget,
    val restrictedToggleAttempt: Boolean = false,
  ) : CommandIntent

  data class ToggleTorch(val enable: Boolean) : CommandIntent

  data object QueryBattery : CommandIntent

  data object QueryTime : CommandIntent

  data object QueryDate : CommandIntent

  data object QueryNetwork : CommandIntent

  data object QueryDevice : CommandIntent

  data class DialNumber(val phoneNumber: String) : CommandIntent

  data class CalculateMath(val expression: String, val formattedResult: String) : CommandIntent

  data class YouTubeSearch(val query: String) : CommandIntent

  data class WebSearch(val query: String) : CommandIntent

  data object Greeting : CommandIntent

  data object HowAreYou : CommandIntent

  data object Help : CommandIntent

  data object ThankYou : CommandIntent

  data class Unsupported(val originalText: String) : CommandIntent
}

sealed interface CommandExecutionResult {
  data class Success(
    val hindiReply: String,
    val englishNote: String,
    val category: String,
    val actionLabel: String? = null,
  ) : CommandExecutionResult

  data class CannotPerform(
    val hindiReason: String,
    val englishReason: String,
    val category: String,
  ) : CommandExecutionResult

  data class PermissionRequired(
    val permission: String,
    val hindiReason: String,
    val englishReason: String,
    val category: String,
    val pendingCommandText: String,
  ) : CommandExecutionResult
}
