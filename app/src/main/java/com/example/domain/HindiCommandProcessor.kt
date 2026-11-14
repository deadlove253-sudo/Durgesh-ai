package com.example.domain

import java.text.DecimalFormat
import java.util.Locale

class HindiCommandProcessor {

  fun parse(rawInput: String): CommandIntent {
    val cleaned = rawInput.trim()
    if (cleaned.isBlank()) return CommandIntent.Unsupported(rawInput)

    val lower = cleaned.lowercase(Locale.ROOT)

    // 1. Direct phone dial command with digits
    extractDialNumber(cleaned, lower)?.let {
      return CommandIntent.DialNumber(it)
    }

    // 2. YouTube search vs direct YouTube open
    if (containsAny(lower, "youtube", "यूट्यूब") && containsAny(lower, "खोजो", "search", "चलाओ", "play", "सर्च", "पर ")) {
      val query = extractSearchQuery(cleaned, listOf("youtube", "यूट्यूब", "par", "पर", "pe", "पे", "mein", "में", "search", "खोजो", "सर्च", "karo", "करो", "chala", "चलाओ", "play", "sunao", "सुनाओ"))
      if (query.isNotBlank()) {
        return CommandIntent.YouTubeSearch(query)
      }
    }

    // 3. Web / Google search
    if (containsAny(lower, "google", "गूगल", "सर्च करो", "search ", "खोजो")) {
      val query = extractSearchQuery(cleaned, listOf("google", "गूगल", "par", "पर", "pe", "पे", "search", "सर्च", "खोजो", "karo", "करो", "batao", "बताओ"))
      if (query.isNotBlank()) {
        return CommandIntent.WebSearch(query)
      }
    }

    // 4. Settings commands (Wi-Fi, Bluetooth, Phone Settings, Display, Sound, Battery, Voice Input)
    parseSettingsCommand(lower)?.let {
      return it
    }

    // 5. Flashlight / Torch commands
    if (containsAny(lower, "टॉर्च", "torch", "फ़्लैशलाइट", "फ्लैशलाइट", "फ्लैश", "flashlight", "flash light", "flash")) {
      val turnOff = containsAny(lower, "बंद", "ऑफ", "off", "band", "stop", "रोको")
      return CommandIntent.ToggleTorch(enable = !turnOff)
    }

    // 6. App Launch commands (YouTube, WhatsApp, Chrome, Camera, etc.)
    parseAppCommand(lower)?.let {
      return it
    }

    // 7. Offline math calculation
    tryEvaluateMath(cleaned, lower)?.let { (expr, result) ->
      return CommandIntent.CalculateMath(expr, result)
    }

    // 8. Offline device queries (Battery, Time, Date, Network, Device info)
    if (containsAny(lower, "बैटरी", "battery", "चार्ज", "charge", "charging")) {
      return CommandIntent.QueryBattery
    }
    if (containsAny(lower, "समय", "बजे", "time", "samay", "baje", "ghadi", "clock") &&
      !containsAny(lower, "अलार्म", "alarm")
    ) {
      return CommandIntent.QueryTime
    }
    if (containsAny(lower, "तारीख", "तारीख़", "दिन", "date", "tarikh", "aaj ka din", "today")) {
      return CommandIntent.QueryDate
    }
    if (containsAny(lower, "नेटवर्क", "इंटरनेट", "network", "internet", "online", "offline", "ऑनलाइन", "ऑफ़लाइन", "कनेक्शन")) {
      return CommandIntent.QueryNetwork
    }
    if (containsAny(lower, "फोन कौन", "डिवाइस", "मॉडल", "poco", "hyperos", "device", "model", "phone info")) {
      return CommandIntent.QueryDevice
    }

    // 9. Conversational greetings & help
    if (containsAny(lower, "कैसे हो", "kaisa hai", "kaise ho", "how are you", "क्या हाल")) {
      return CommandIntent.HowAreYou
    }
    if (containsAny(lower, "मदद", "हेल्प", "help", "madad", "क्या कर सकते", "commands", "फीचर")) {
      return CommandIntent.Help
    }
    if (containsAny(lower, "धन्यवाद", "शुक्रिया", "थैंक", "thanks", "thank you", "shukriya", "dhanyawad")) {
      return CommandIntent.ThankYou
    }
    if (containsAny(lower, "नमस्ते", "नमस्कार", "हेलो", "हाय", "hello", "hi ", "namaste", "namaskar", "तुम कौन", "तुम्हारा नाम", "who are you", "durgesh", "दुर्गेश")) {
      return CommandIntent.Greeting
    }

    return CommandIntent.Unsupported(cleaned)
  }

  private fun parseSettingsCommand(lower: String): CommandIntent.OpenSettings? {
    val toggleWords = arrayOf("चालू", "ऑन", "बंद", "ऑफ", "on", "off", "chalu", "band", "enable", "disable", "toggle", "connect")
    val isToggleAttempt = containsAny(lower, *toggleWords) && !containsAny(lower, "सेटिंग", "settings", "panel", "पैनल", "खोलो", "open", "kholo")

    return when {
      containsAny(lower, "वाई-फ़ाई", "वाईफाई", "वाई फाई", "wifi", "wi-fi") ->
        CommandIntent.OpenSettings(SettingsTarget.WIFI, restrictedToggleAttempt = isToggleAttempt)

      containsAny(lower, "ब्लूटूथ", "bluetooth") ->
        CommandIntent.OpenSettings(SettingsTarget.BLUETOOTH, restrictedToggleAttempt = isToggleAttempt)

      containsAny(lower, "मोबाइल डेटा", "mobile data", "इंटरनेट पैनल", "internet settings", "hotspot", "हॉटस्पॉट") ->
        CommandIntent.OpenSettings(SettingsTarget.INTERNET_PANEL, restrictedToggleAttempt = isToggleAttempt)

      containsAny(lower, "आवाज़ सेटिंग", "साउंड सेटिंग", "वॉल्यूम", "sound setting", "volume") ->
        CommandIntent.OpenSettings(SettingsTarget.VOLUME)

      containsAny(lower, "डिस्प्ले", "ब्राइटनेस", "स्क्रीन सेटिंग", "display", "brightness") ->
        CommandIntent.OpenSettings(SettingsTarget.DISPLAY)

      containsAny(lower, "बैटरी सेटिंग", "battery setting", "battery saver", "बैटरी सेवर") ->
        CommandIntent.OpenSettings(SettingsTarget.BATTERY)

      containsAny(lower, "वॉइस सेटिंग", "ऑफलाइन पैक", "speech setting", "voice input", "tts") ->
        CommandIntent.OpenSettings(SettingsTarget.VOICE_INPUT)

      containsAny(lower, "सेटिंग्स", "सेटिंग", "settings", "setting") ->
        CommandIntent.OpenSettings(SettingsTarget.PHONE_SETTINGS)

      else -> null
    }
  }

  private fun parseAppCommand(lower: String): CommandIntent.OpenApp? {
    val target =
      when {
        containsAny(lower, "youtube", "यूट्यूब") -> AppTarget.YOUTUBE
        containsAny(lower, "whatsapp", "व्हाट्सएप", "व्हाट्सऐप", "वाट्सएप") -> AppTarget.WHATSAPP
        containsAny(lower, "chrome", "क्रोम", "ब्राउज़र", "browser") -> AppTarget.CHROME
        containsAny(lower, "camera", "कैमरा", "फ़ोटो खींच", "फोटो खींच", "सेल्फी", "selfie") -> AppTarget.CAMERA
        containsAny(lower, "calculator", "कैलकुलेटर", "हिसाब") -> AppTarget.CALCULATOR
        containsAny(lower, "messages", "मैसेज", "sms", "एसएमएस") -> AppTarget.MESSAGES
        containsAny(lower, "dialer", "डायलर", "फ़ोन ऐप", "फोन ऐप") -> AppTarget.DIALER
        containsAny(lower, "maps", "मैप्स", "नक्शा", "रास्ता") -> AppTarget.MAPS
        containsAny(lower, "alarm", "अलार्म", "घड़ी", "clock") -> AppTarget.ALARMS
        containsAny(lower, "play store", "प्ले स्टोर") -> AppTarget.PLAY_STORE
        containsAny(lower, "gallery", "गैलरी", "फ़ोटो देखो", "फोटो देखो") -> AppTarget.GALLERY
        else -> null
      }
    return target?.let { CommandIntent.OpenApp(it) }
  }

  private fun extractDialNumber(original: String, lower: String): String? {
    if (!containsAny(lower, "कॉल", "call", "dial", "डायल", "फोन लगाओ", "फ़ोन लगाओ")) return null
    val digits = original.filter { it.isDigit() || it == '+' }
    return if (digits.count { it.isDigit() } >= 3) digits else null
  }

  private fun extractSearchQuery(original: String, stopTokens: List<String>): String {
    var result = original
    for (token in stopTokens) {
      result = result.replace(Regex("(?i)\\b${Regex.escape(token)}\\b"), " ")
      result = result.replace(token, " ", ignoreCase = true)
    }
    return result.replace(Regex("\\s+"), " ").trim()
  }

  private fun tryEvaluateMath(original: String, lower: String): Pair<String, String>? {
    var expr = lower
      .replace("जमा", "+")
      .replace("जोड़ो", "+")
      .replace("प्लस", "+")
      .replace("plus", "+")
      .replace("घटाओ", "-")
      .replace("माइनस", "-")
      .replace("minus", "-")
      .replace("गुना", "*")
      .replace("गुणा", "*")
      .replace("into", "*")
      .replace("multiply", "*")
      .replace("x", "*")
      .replace("×", "*")
      .replace("भाग", "/")
      .replace("divide by", "/")
      .replace("divided by", "/")
      .replace("÷", "/")
      .replace("कितना होता है", "")
      .replace("कितने होते हैं", "")
      .replace("बराबर", "")
      .trim()

    val match = Regex("""(-?\d+(?:\.\d+)?)\s*([+\-*/])\s*(-?\d+(?:\.\d+)?)""").find(expr)
      ?: return null

    val left = match.groupValues[1].toDoubleOrNull() ?: return null
    val op = match.groupValues[2]
    val right = match.groupValues[3].toDoubleOrNull() ?: return null

    if (op == "/" && right == 0.0) {
      return "${match.groupValues[1]} ÷ 0" to "शून्य से भाग नहीं दिया जा सकता (Cannot divide by zero)"
    }

    val value =
      when (op) {
        "+" -> left + right
        "-" -> left - right
        "*" -> left * right
        "/" -> left / right
        else -> return null
      }

    val formatter = DecimalFormat("#,##0.####")
    val formatted = formatter.format(value)
    return "${match.groupValues[1]} $op ${match.groupValues[3]}" to formatted
  }

  private fun containsAny(text: String, vararg keywords: String): Boolean {
    return keywords.any { text.contains(it) }
  }
}
