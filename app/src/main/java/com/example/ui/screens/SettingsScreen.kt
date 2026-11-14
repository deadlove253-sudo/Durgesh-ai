package com.example.ui.screens

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AssistantPreferences
import com.example.domain.SettingsTarget
import com.example.system.DeviceStatusSnapshot
import com.example.system.VoiceEngineCapabilities
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSpaceBlue
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MistBlue
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite

@Composable
fun SettingsScreen(
  preferences: AssistantPreferences,
  capabilities: VoiceEngineCapabilities,
  deviceStatus: DeviceStatusSnapshot,
  micGranted: Boolean,
  cameraGranted: Boolean,
  onToggleAutoSpeak: (Boolean) -> Unit,
  onTogglePreferOffline: (Boolean) -> Unit,
  onSpeechRateChange: (Float) -> Unit,
  onRequestPermission: (String) -> Unit,
  onOpenAppSettings: () -> Unit,
  onOpenSystemSettings: (SettingsTarget) -> Unit,
  onClearChatHistory: () -> Unit,
  onClearVoiceHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(listOf(MidnightVoid, DeepSpaceBlue, MidnightVoid))
        ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(
          text = "सेटिंग्स और अनुमतियाँ",
          style = MaterialTheme.typography.headlineMedium,
          color = StarlightWhite,
        )
        Text(
          text = "हिंदी वॉइस इंजन, प्राइवेसी अनुमतियाँ और POCO HyperOS टिप्स",
          style = MaterialTheme.typography.bodyMedium,
          color = MistBlue,
        )
      }

      // Voice & Speech Preferences
      item {
        SettingsCard(title = "वॉइस और स्पीच सेटिंग्स", icon = Icons.AutoMirrored.Filled.VolumeUp) {
          PreferenceToggleRow(
            title = "हिंदी वॉइस में जवाब बोलें (TextToSpeech)",
            subtitle = capabilities.ttsStatusNote,
            checked = preferences.autoSpeakHindi,
            onCheckedChange = onToggleAutoSpeak,
            testTag = "toggle_auto_speak",
          )
          Spacer(modifier = Modifier.height(10.dp))
          PreferenceToggleRow(
            title = "ऑफ़लाइन स्पीच रिकग्निशन प्राथमिकता",
            subtitle = "डिवाइस पर ऑफ़लाइन हिंदी पैक उपलब्ध होने पर बिना इंटरनेट बोलकर कमांड दें",
            checked = preferences.preferOfflineSpeech,
            onCheckedChange = onTogglePreferOffline,
            testTag = "toggle_prefer_offline",
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Speed,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "बोलने की गति: ${"%.1fx".format(preferences.speechRate)}",
              style = MaterialTheme.typography.titleSmall,
              color = StarlightWhite,
            )
          }
          Slider(
            value = preferences.speechRate,
            onValueChange = onSpeechRateChange,
            valueRange = 0.7f..1.4f,
            steps = 6,
            colors =
              SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = NeonPurple,
                inactiveTrackColor = CosmicBorder,
              ),
            modifier = Modifier.testTag("speech_rate_slider"),
          )
        }
      }

      // Permissions & Privacy Card
      item {
        SettingsCard(title = "अनुमतियाँ और गोपनीयता", icon = Icons.Filled.Security) {
          Text(
            text =
              "Durgesh AI कोई छिपी हुई बैकग्राउंड रिकॉर्डिंग नहीं करता और न ही Android / HyperOS सुरक्षा नियमों को बायपास करता है।",
            style = MaterialTheme.typography.bodySmall,
            color = CyberCyan,
          )
          Spacer(modifier = Modifier.height(12.dp))

          PermissionStatusRow(
            icon = Icons.Filled.Mic,
            name = "माइक्रोफ़ोन (RECORD_AUDIO)",
            description = "केवल माइक बटन दबाने पर हिंदी आवाज़ सुनने के लिए",
            granted = micGranted,
            onActionClick = {
              if (micGranted) onOpenAppSettings()
              else onRequestPermission(Manifest.permission.RECORD_AUDIO)
            },
            testTag = "perm_mic_button",
          )

          Spacer(modifier = Modifier.height(10.dp))

          PermissionStatusRow(
            icon = Icons.Filled.CameraAlt,
            name = "कैमरा हार्डवेयर (CAMERA)",
            description = "डिवाइस की टॉर्च / फ़्लैशलाइट चालू-बंद करने के लिए",
            granted = cameraGranted,
            onActionClick = {
              if (cameraGranted) onOpenAppSettings()
              else onRequestPermission(Manifest.permission.CAMERA)
            },
            testTag = "perm_camera_button",
          )
        }
      }

      // POCO HyperOS Guide
      item {
        SettingsCard(title = "POCO HyperOS गाइड", icon = Icons.Filled.PhoneAndroid) {
          Text(
            text =
              "• डिवाइस: ${deviceStatus.deviceModelLabel} (${deviceStatus.osVersionLabel})\n" +
                "• ऑफ़लाइन हिंदी वॉइस के लिए Google Voice Input सेटिंग्स से 'Hindi (India)' ऑफ़लाइन पैक डाउनलोड करें।\n" +
                "• HyperOS सुरक्षा नियमों के कारण Wi-Fi / Bluetooth सीधे बैकग्राउंड में नहीं बदले जाते; Durgesh AI आपके लिए सुरक्षित सिस्टम पैनल खोलता है।",
            style = MaterialTheme.typography.bodySmall,
            color = MistBlue,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { onOpenSystemSettings(SettingsTarget.VOICE_INPUT) },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = DeepVioletContainer,
                  contentColor = StarlightWhite,
                ),
              border = BorderStroke(1.dp, NeonPurple),
              modifier = Modifier.weight(1f).testTag("open_voice_input_settings"),
            ) {
              Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("वॉइस पैक सेटिंग्स")
            }

            OutlinedButton(
              onClick = onOpenAppSettings,
              border = BorderStroke(1.dp, CyberCyan),
              modifier = Modifier.weight(1f).testTag("open_app_details_settings"),
            ) {
              Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("App Settings", color = CyberCyan)
            }
          }
        }
      }

      // History Cleanup
      item {
        SettingsCard(title = "डेटा और इतिहास प्रबंधन", icon = Icons.Filled.DeleteForever) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onClearChatHistory,
              border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.7f)),
              modifier = Modifier.weight(1f).testTag("settings_clear_chat"),
            ) {
              Text("चैट इतिहास साफ़", color = CoralAlert)
            }
            OutlinedButton(
              onClick = onClearVoiceHistory,
              border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.7f)),
              modifier = Modifier.weight(1f).testTag("settings_clear_voice_history"),
            ) {
              Text("वॉइस इतिहास साफ़", color = CoralAlert)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = CosmicNavy,
    border = BorderStroke(1.dp, CosmicBorder),
    tonalElevation = 3.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = StarlightWhite,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun PreferenceToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall, color = StarlightWhite)
      Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MistBlue)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = MidnightVoid,
          checkedTrackColor = CyberCyan,
          uncheckedThumbColor = MistBlue,
          uncheckedTrackColor = NebulaSurface,
        ),
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
private fun PermissionStatusRow(
  icon: ImageVector,
  name: String,
  description: String,
  granted: Boolean,
  onActionClick: () -> Unit,
  testTag: String,
) {
  Surface(
    color = NebulaSurface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, if (granted) EmeraldGlow.copy(alpha = 0.45f) else AmberPulse.copy(alpha = 0.5f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (granted) EmeraldGlow else AmberPulse,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = name, style = MaterialTheme.typography.titleSmall, color = StarlightWhite)
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MistBlue)
      }
      Spacer(modifier = Modifier.width(8.dp))
      if (granted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "अनुमति प्राप्त",
            tint = EmeraldGlow,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("चालू", style = MaterialTheme.typography.labelMedium, color = EmeraldGlow)
        }
      } else {
        Button(
          onClick = onActionClick,
          colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = MidnightVoid),
          modifier = Modifier.testTag(testTag),
        ) {
          Text("अनुमति दें")
        }
      }
    }
  }
}
