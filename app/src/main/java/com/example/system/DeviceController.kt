package com.example.system

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.domain.AppTarget
import com.example.domain.CommandExecutionResult
import com.example.domain.CommandIntent
import com.example.domain.SettingsTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceStatusSnapshot(
  val batteryPercent: Int,
  val isCharging: Boolean,
  val isOnline: Boolean,
  val networkTypeLabel: String,
  val deviceModelLabel: String,
  val isHyperOsOrMiui: Boolean,
  val osVersionLabel: String,
)

class DeviceController(private val context: Context) {

  private var torchEnabledState: Boolean = false

  fun isMicrophoneGranted(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED
  }

  fun isCameraGranted(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED
  }

  fun getDeviceStatusSnapshot(): DeviceStatusSnapshot {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else -1
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    val (isOnline, netLabel) = readNetworkState()
    val brand = Build.BRAND.replaceFirstChar { it.uppercase(Locale.ROOT) }
    val model = Build.MODEL
    val hyperOsLabel = detectHyperOsLabel()

    return DeviceStatusSnapshot(
      batteryPercent = percent,
      isCharging = isCharging,
      isOnline = isOnline,
      networkTypeLabel = netLabel,
      deviceModelLabel = "$brand $model",
      isHyperOsOrMiui = hyperOsLabel != null,
      osVersionLabel = hyperOsLabel ?: "Android ${Build.VERSION.RELEASE}",
    )
  }

  fun execute(intent: CommandIntent, originalText: String): CommandExecutionResult {
    return when (intent) {
      is CommandIntent.OpenApp -> launchApp(intent.target)
      is CommandIntent.OpenSettings -> openSettingsScreen(intent.target, intent.restrictedToggleAttempt)
      is CommandIntent.ToggleTorch -> toggleTorch(intent.enable, originalText)
      CommandIntent.QueryBattery -> handleBatteryQuery()
      CommandIntent.QueryTime -> handleTimeQuery()
      CommandIntent.QueryDate -> handleDateQuery()
      CommandIntent.QueryNetwork -> handleNetworkQuery()
      CommandIntent.QueryDevice -> handleDeviceQuery()
      is CommandIntent.DialNumber -> dialNumber(intent.phoneNumber)
      is CommandIntent.CalculateMath ->
        CommandExecutionResult.Success(
          hindiReply = "${intent.expression} का उत्तर ${intent.formattedResult} है।",
          englishNote = "Offline Calculation: ${intent.expression} = ${intent.formattedResult}",
          category = "ऑफ़लाइन गणित",
          actionLabel = "Offline Math",
        )
      is CommandIntent.YouTubeSearch -> launchYouTubeSearch(intent.query)
      is CommandIntent.WebSearch -> launchWebSearch(intent.query)
      CommandIntent.Greeting ->
        CommandExecutionResult.Success(
          hindiReply = "नमस्ते! मैं दुर्गेश एआई हूँ, आपका POCO HyperOS हिंदी वॉइस असिस्टेंट। बताइए मैं आपकी क्या मदद करूँ?",
          englishNote = "Durgesh AI ready for Hindi voice & offline commands.",
          category = "अभिवादन",
        )
      CommandIntent.HowAreYou ->
        CommandExecutionResult.Success(
          hindiReply = "मैं बिल्कुल ठीक हूँ और आपकी मदद के लिए तैयार हूँ! आप मुझसे ऐप्स खोलने, सेटिंग्स दिखाने या बैटरी और समय पूछ सकते हैं।",
          englishNote = "System healthy and ready.",
          category = "बातचीत",
        )
      CommandIntent.Help ->
        CommandExecutionResult.Success(
          hindiReply =
            "आप बोल सकते हैं: यूट्यूब खोलो, व्हाट्सएप खोलो, क्रोम खोलो, कैमरा खोलो, वाई-फ़ाई सेटिंग्स, ब्लूटूथ सेटिंग्स, बैटरी कितनी है, समय क्या हुआ है, या टॉर्च जलाओ।",
          englishNote = "Try app intents, settings panels, or offline utilities.",
          category = "सहायता",
        )
      CommandIntent.ThankYou ->
        CommandExecutionResult.Success(
          hindiReply = "आपका स्वागत है! जब भी जरूरत हो, बस माइक दबाकर बोलिए।",
          englishNote = "You're welcome!",
          category = "बातचीत",
        )
      is CommandIntent.Unsupported ->
        CommandExecutionResult.CannotPerform(
          hindiReason =
            "माफ़ कीजिए, \"${intent.originalText}\" कमांड अभी पूरा नहीं किया जा सकता। मैं ऐप्स (YouTube, WhatsApp, Chrome, Camera), Wi-Fi/Bluetooth/Phone सेटिंग्स, टॉर्च, बैटरी, समय-तारीख और आसान गणित कर सकता हूँ।",
          englishReason = "Command not supported offline or outside Android security boundaries.",
          category = "असमर्थ कमांड",
        )
    }
  }

  fun launchApp(target: AppTarget): CommandExecutionResult {
    val pm = context.packageManager

    // 1. Try known package names first
    for (pkg in target.packages) {
      val launchIntent = pm.getLaunchIntentForPackage(pkg)
      if (launchIntent != null) {
        return startIntentSafely(
          intent = launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          hindiSuccess = "${target.hindiName} खोल रहा हूँ।",
          englishSuccess = "Opening ${target.englishName}",
          category = "ऐप लॉन्च",
          actionLabel = target.englishName,
          notFoundHindi = "${target.hindiName} ऐप आपके फोन में इंस्टॉल नहीं है।",
          notFoundEnglish = "${target.englishName} is not installed on this device.",
        )
      }
    }

    // 2. Fallback implicit intents for system apps
    val implicitIntent: Intent? =
      when (target) {
        AppTarget.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        AppTarget.DIALER -> Intent(Intent.ACTION_DIAL)
        AppTarget.MESSAGES ->
          Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
        AppTarget.CALCULATOR ->
          Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
        AppTarget.GALLERY ->
          Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
        AppTarget.ALARMS -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        AppTarget.PLAY_STORE ->
          Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store"))
        else -> null
      }

    if (implicitIntent != null && implicitIntent.resolveActivity(pm) != null) {
      return startIntentSafely(
        intent = implicitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        hindiSuccess = "${target.hindiName} खोल रहा हूँ।",
        englishSuccess = "Opening ${target.englishName}",
        category = "ऐप लॉन्च",
        actionLabel = target.englishName,
        notFoundHindi = "${target.hindiName} ऐप आपके फोन पर उपलब्ध नहीं मिला।",
        notFoundEnglish = "No compatible ${target.englishName} app found.",
      )
    }

    return CommandExecutionResult.CannotPerform(
      hindiReason = "${target.hindiName} ऐप आपके फोन में इंस्टॉल नहीं मिला, इसलिए इसे खोला नहीं जा सका।",
      englishReason = "${target.englishName} app is not installed on this device.",
      category = "ऐप लॉन्च",
    )
  }

  fun openSettingsScreen(
    target: SettingsTarget,
    restrictedToggleAttempt: Boolean = false,
  ): CommandExecutionResult {
    val pm = context.packageManager
    val candidateActions =
      when (target) {
        SettingsTarget.WIFI ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS)
          } else {
            listOf(Settings.ACTION_WIFI_SETTINGS)
          }
        SettingsTarget.BLUETOOTH -> listOf(Settings.ACTION_BLUETOOTH_SETTINGS)
        SettingsTarget.PHONE_SETTINGS -> listOf(Settings.ACTION_SETTINGS)
        SettingsTarget.INTERNET_PANEL ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, Settings.ACTION_WIRELESS_SETTINGS)
          } else {
            listOf(Settings.ACTION_WIRELESS_SETTINGS)
          }
        SettingsTarget.VOLUME ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Settings.Panel.ACTION_VOLUME, Settings.ACTION_SOUND_SETTINGS)
          } else {
            listOf(Settings.ACTION_SOUND_SETTINGS)
          }
        SettingsTarget.DISPLAY -> listOf(Settings.ACTION_DISPLAY_SETTINGS)
        SettingsTarget.BATTERY ->
          listOf(Settings.ACTION_BATTERY_SAVER_SETTINGS, Intent.ACTION_POWER_USAGE_SUMMARY)
        SettingsTarget.VOICE_INPUT ->
          listOf(Settings.ACTION_VOICE_INPUT_SETTINGS, "com.android.settings.TTS_SETTINGS")
      }

    for (action in candidateActions) {
      val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (intent.resolveActivity(pm) != null) {
        val hindiReply =
          if (restrictedToggleAttempt) {
            "Android और HyperOS सुरक्षा नियमों के कारण ऐप्स सीधे टॉगल नहीं बदल सकते। मैंने ${target.hindiName} खोल दी है, आप यहाँ से बदल सकते हैं।"
          } else {
            "${target.hindiName} खोल रहा हूँ।"
          }
        val englishNote =
          if (restrictedToggleAttempt) {
            "Opened ${target.englishName} (direct background toggle restricted by Android/HyperOS)."
          } else {
            "Opened ${target.englishName}"
          }
        return startIntentSafely(
          intent = intent,
          hindiSuccess = hindiReply,
          englishSuccess = englishNote,
          category = "फ़ोन सेटिंग्स",
          actionLabel = target.englishName,
          notFoundHindi = "${target.hindiName} स्क्रीन खोली नहीं जा सकी।",
          notFoundEnglish = "Could not open ${target.englishName}.",
        )
      }
    }

    return CommandExecutionResult.CannotPerform(
      hindiReason = "आपके डिवाइस पर ${target.hindiName} स्क्रीन उपलब्ध नहीं मिली।",
      englishReason = "${target.englishName} screen is unavailable on this device.",
      category = "फ़ोन सेटिंग्स",
    )
  }

  fun openAppPermissionSettings(): Boolean {
    return try {
      val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
          )
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun toggleTorch(enable: Boolean, originalText: String): CommandExecutionResult {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
      return CommandExecutionResult.CannotPerform(
        hindiReason = "इस डिवाइस में फ़्लैशलाइट (Torch) हार्डवेयर उपलब्ध नहीं है।",
        englishReason = "No camera flash hardware found on this device.",
        category = "टॉर्च",
      )
    }

    if (!isCameraGranted()) {
      return CommandExecutionResult.PermissionRequired(
        permission = Manifest.permission.CAMERA,
        hindiReason = "फ़्लैशलाइट नियंत्रित करने के लिए कैमरा अनुमति (Camera Permission) आवश्यक है।",
        englishReason = "Camera permission is requested before accessing the device flashlight.",
        category = "टॉर्च",
        pendingCommandText = originalText,
      )
    }

    return try {
      val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
      val cameraId =
        cameraManager.cameraIdList.firstOrNull { id ->
          val chars = cameraManager.getCameraCharacteristics(id)
          chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
      if (cameraId == null) {
        CommandExecutionResult.CannotPerform(
          hindiReason = "फ़्लैशलाइट के लिए कोई संगत कैमरा यूनिट नहीं मिली।",
          englishReason = "No camera with flash unit found.",
          category = "टॉर्च",
        )
      } else {
        cameraManager.setTorchMode(cameraId, enable)
        torchEnabledState = enable
        val stateHindi = if (enable) "चालू" else "बंद"
        val stateEng = if (enable) "ON" else "OFF"
        CommandExecutionResult.Success(
          hindiReply = "फ़्लैशलाइट $stateHindi कर दी गई है।",
          englishNote = "Flashlight turned $stateEng",
          category = "टॉर्च",
          actionLabel = "Torch $stateEng",
        )
      }
    } catch (_: SecurityException) {
      CommandExecutionResult.CannotPerform(
        hindiReason = "सिस्टम सुरक्षा नियम के कारण फ़्लैशलाइट नहीं बदली जा सकी।",
        englishReason = "Torch access denied by system security policy.",
        category = "टॉर्च",
      )
    } catch (e: Exception) {
      CommandExecutionResult.CannotPerform(
        hindiReason = "फ़्लैशलाइट अभी नियंत्रित नहीं हो सकी (हो सकता है कैमरा किसी दूसरे ऐप में चालू हो)।",
        englishReason = "Torch error: ${e.localizedMessage ?: "Camera in use"}",
        category = "टॉर्च",
      )
    }
  }

  private fun handleBatteryQuery(): CommandExecutionResult {
    val snapshot = getDeviceStatusSnapshot()
    if (snapshot.batteryPercent < 0) {
      return CommandExecutionResult.CannotPerform(
        hindiReason = "बैटरी स्तर पढ़ा नहीं जा सका।",
        englishReason = "Unable to read battery telemetry.",
        category = "डिवाइस जानकारी",
      )
    }
    val chargingHindi = if (snapshot.isCharging) "और फोन चार्ज हो रहा है" else "और अभी चार्जिंग पर नहीं है"
    val chargingEng = if (snapshot.isCharging) "Charging" else "Not charging"
    return CommandExecutionResult.Success(
      hindiReply = "आपके फोन की बैटरी ${snapshot.batteryPercent} प्रतिशत है $chargingHindi।",
      englishNote = "Battery: ${snapshot.batteryPercent}% ($chargingEng)",
      category = "डिवाइस जानकारी",
      actionLabel = "${snapshot.batteryPercent}% Battery",
    )
  }

  private fun handleTimeQuery(): CommandExecutionResult {
    val now = Date()
    val hindiTime = SimpleDateFormat("h:mm a", Locale("hi", "IN")).format(now)
    val engTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(now)
    return CommandExecutionResult.Success(
      hindiReply = "अभी समय $hindiTime हुआ है।",
      englishNote = "Current time: $engTime",
      category = "समय और तारीख",
      actionLabel = engTime,
    )
  }

  private fun handleDateQuery(): CommandExecutionResult {
    val now = Date()
    val hindiDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("hi", "IN")).format(now)
    val engDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).format(now)
    return CommandExecutionResult.Success(
      hindiReply = "आज $hindiDate है।",
      englishNote = "Today: $engDate",
      category = "समय और तारीख",
      actionLabel = engDate,
    )
  }

  private fun handleNetworkQuery(): CommandExecutionResult {
    val (online, label) = readNetworkState()
    val hindi =
      if (online) {
        "आपका फोन अभी $label के माध्यम से इंटरनेट से जुड़ा है।"
      } else {
        "अभी आपका फोन ऑफ़लाइन है। इंटरनेट कनेक्शन उपलब्ध नहीं है।"
      }
    return CommandExecutionResult.Success(
      hindiReply = hindi,
      englishNote = "Network: $label",
      category = "डिवाइस जानकारी",
      actionLabel = label,
    )
  }

  private fun handleDeviceQuery(): CommandExecutionResult {
    val snapshot = getDeviceStatusSnapshot()
    return CommandExecutionResult.Success(
      hindiReply =
        "यह ${snapshot.deviceModelLabel} डिवाइस है, जिस पर ${snapshot.osVersionLabel} (Android ${Build.VERSION.RELEASE}) चल रहा है।",
      englishNote = "${snapshot.deviceModelLabel} • ${snapshot.osVersionLabel}",
      category = "डिवाइस जानकारी",
      actionLabel = snapshot.deviceModelLabel,
    )
  }

  private fun dialNumber(phoneNumber: String): CommandExecutionResult {
    val intent =
      Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return startIntentSafely(
      intent = intent,
      hindiSuccess = "नंबर $phoneNumber डायलर में खोल रहा हूँ। कॉल शुरू करने के लिए कॉल बटन दबाएँ।",
      englishSuccess = "Opened Phone Dialer with $phoneNumber",
      category = "कॉल डायलर",
      actionLabel = "Dial $phoneNumber",
      notFoundHindi = "आपके डिवाइस पर फ़ोन डायलर ऐप उपलब्ध नहीं है।",
      notFoundEnglish = "Phone dialer app is not available.",
    )
  }

  private fun launchYouTubeSearch(query: String): CommandExecutionResult {
    val (online, _) = readNetworkState()
    if (!online) {
      return CommandExecutionResult.CannotPerform(
        hindiReason = "यूट्यूब पर \"$query\" खोजने के लिए इंटरनेट कनेक्शन आवश्यक है। अभी फोन ऑफ़लाइन है।",
        englishReason = "Internet connection required for YouTube search.",
        category = "यूट्यूब सर्च",
      )
    }
    val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return startIntentSafely(
      intent = intent,
      hindiSuccess = "यूट्यूब पर \"$query\" खोज रहा हूँ।",
      englishSuccess = "Searching YouTube for \"$query\"",
      category = "यूट्यूब सर्च",
      actionLabel = "YouTube Search",
      notFoundHindi = "यूट्यूब या ब्राउज़र ऐप खोलने में समस्या आई।",
      notFoundEnglish = "No app found to open YouTube search.",
    )
  }

  private fun launchWebSearch(query: String): CommandExecutionResult {
    val (online, _) = readNetworkState()
    if (!online) {
      return CommandExecutionResult.CannotPerform(
        hindiReason = "इंटरनेट सर्च के लिए नेटवर्क कनेक्शन आवश्यक है। अभी फोन ऑफ़लाइन है।",
        englishReason = "Device is offline; web search cannot run.",
        category = "वेब सर्च",
      )
    }
    val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return startIntentSafely(
      intent = intent,
      hindiSuccess = "गूगल पर \"$query\" खोज रहा हूँ।",
      englishSuccess = "Searching Google for \"$query\"",
      category = "वेब सर्च",
      actionLabel = "Google Search",
      notFoundHindi = "वेब सर्च खोलने के लिए कोई ब्राउज़र ऐप नहीं मिला।",
      notFoundEnglish = "No browser app found to open search.",
    )
  }

  private fun startIntentSafely(
    intent: Intent,
    hindiSuccess: String,
    englishSuccess: String,
    category: String,
    actionLabel: String,
    notFoundHindi: String,
    notFoundEnglish: String,
  ): CommandExecutionResult {
    return try {
      context.startActivity(intent)
      CommandExecutionResult.Success(
        hindiReply = hindiSuccess,
        englishNote = englishSuccess,
        category = category,
        actionLabel = actionLabel,
      )
    } catch (_: ActivityNotFoundException) {
      CommandExecutionResult.CannotPerform(
        hindiReason = notFoundHindi,
        englishReason = notFoundEnglish,
        category = category,
      )
    } catch (_: SecurityException) {
      CommandExecutionResult.CannotPerform(
        hindiReason = "Android / HyperOS सुरक्षा नियम ने इस ऐप को सीधे खोलने से रोका है।",
        englishReason = "Blocked by Android/HyperOS security restriction.",
        category = category,
      )
    }
  }

  private fun readNetworkState(): Pair<Boolean, String> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return false to "Offline"
    val network = cm.activeNetwork ?: return false to "Offline"
    val caps = cm.getNetworkCapabilities(network) ?: return false to "Offline"
    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    if (!hasInternet) return false to "Offline"
    return when {
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true to "Wi-Fi"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true to "Mobile Data"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true to "Ethernet"
      else -> true to "Connected"
    }
  }

  private fun detectHyperOsLabel(): String? {
    return try {
      val sysProps = Class.forName("android.os.SystemProperties")
      val getMethod = sysProps.getMethod("get", String::class.java, String::class.java)
      val hyperVersion = getMethod.invoke(null, "ro.mi.os.version.name", "") as String
      val miuiVersion = getMethod.invoke(null, "ro.miui.ui.version.name", "") as String
      when {
        hyperVersion.isNotBlank() -> "HyperOS $hyperVersion"
        miuiVersion.isNotBlank() -> "HyperOS / MIUI $miuiVersion"
        else -> null
      }
    } catch (_: Exception) {
      null
    }
  }
}
