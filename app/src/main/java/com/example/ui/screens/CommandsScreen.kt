package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CommandExecutionStatus
import com.example.data.local.VoiceCommandHistoryEntity
import com.example.domain.AppTarget
import com.example.domain.SettingsTarget
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSpaceBlue
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MistBlue
import com.example.ui.theme.MutedSlateBlue
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickLauncherItem(
  val titleHindi: String,
  val subtitle: String,
  val icon: ImageVector,
  val accent: Color,
  val onClick: () -> Unit,
)

@Composable
fun CommandsScreen(
  voiceHistory: List<VoiceCommandHistoryEntity>,
  selectedFilter: String,
  onSelectFilter: (String) -> Unit,
  onLaunchApp: (AppTarget) -> Unit,
  onOpenSettings: (SettingsTarget) -> Unit,
  onRunCommand: (String) -> Unit,
  onDeleteHistoryItem: (Long) -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val filteredHistory =
    when (selectedFilter) {
      "SUCCESS" -> voiceHistory.filter { it.executionStatus == CommandExecutionStatus.SUCCESS.name }
      "FAILED" -> voiceHistory.filter { it.executionStatus != CommandExecutionStatus.SUCCESS.name }
      else -> voiceHistory
    }

  val appLaunchers =
    listOf(
      QuickLauncherItem("YouTube", "यूट्यूब खोलो", Icons.Filled.PlayCircle, CoralAlert) {
        onLaunchApp(AppTarget.YOUTUBE)
      },
      QuickLauncherItem("WhatsApp", "व्हाट्सएप खोलो", Icons.Filled.Chat, EmeraldGlow) {
        onLaunchApp(AppTarget.WHATSAPP)
      },
      QuickLauncherItem("Chrome", "क्रोम खोलो", Icons.Filled.Language, CyberCyan) {
        onLaunchApp(AppTarget.CHROME)
      },
      QuickLauncherItem("Camera", "कैमरा खोलो", Icons.Filled.CameraAlt, NeonPurple) {
        onLaunchApp(AppTarget.CAMERA)
      },
    )

  val settingsLaunchers =
    listOf(
      QuickLauncherItem("Wi-Fi", "वाई-फ़ाई सेटिंग्स", Icons.Filled.Wifi, CyberCyan) {
        onOpenSettings(SettingsTarget.WIFI)
      },
      QuickLauncherItem("Bluetooth", "ब्लूटूथ सेटिंग्स", Icons.Filled.Bluetooth, NeonPurple) {
        onOpenSettings(SettingsTarget.BLUETOOTH)
      },
      QuickLauncherItem("Settings", "फ़ोन सेटिंग्स", Icons.Filled.Settings, MistBlue) {
        onOpenSettings(SettingsTarget.PHONE_SETTINGS)
      },
      QuickLauncherItem("Torch", "टॉर्च जलाओ", Icons.Filled.FlashlightOn, AmberPulse) {
        onRunCommand("टॉर्च जलाओ")
      },
      QuickLauncherItem("Battery", "बैटरी कितनी है", Icons.Filled.BatteryChargingFull, EmeraldGlow) {
        onRunCommand("बैटरी कितनी है")
      },
      QuickLauncherItem("Time", "समय क्या हुआ है", Icons.Filled.Schedule, CyberCyan) {
        onRunCommand("समय क्या हुआ है")
      },
    )

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
          text = "कमांड्स और वॉइस इतिहास",
          style = MaterialTheme.typography.headlineMedium,
          color = StarlightWhite,
        )
        Text(
          text = "ऐप्स लॉन्च करें, फ़ोन सेटिंग्स खोलें और अपने पुराने वॉइस कमांड देखें",
          style = MaterialTheme.typography.bodyMedium,
          color = MistBlue,
        )
      }

      // App Launchers Section
      item {
        Text(
          text = "ऐप्स खोलें (Android Intents)",
          style = MaterialTheme.typography.titleMedium,
          color = CyberCyan,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(appLaunchers) { item ->
            LauncherActionCard(item = item)
          }
        }
      }

      // Settings & Offline Utilities Section
      item {
        Text(
          text = "सेटिंग्स और ऑफ़लाइन टूल्स",
          style = MaterialTheme.typography.titleMedium,
          color = NeonPurple,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(settingsLaunchers) { item ->
            LauncherActionCard(item = item)
          }
        }
      }

      // Voice Command History Header + Filters
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.History,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "वॉइस कमांड इतिहास (${voiceHistory.size})",
              style = MaterialTheme.typography.titleMedium,
              color = StarlightWhite,
            )
          }
          if (voiceHistory.isNotEmpty()) {
            TextButton(
              onClick = onClearHistory,
              modifier = Modifier.testTag("clear_voice_history_button"),
            ) {
              Text("सब साफ़ करें", color = CoralAlert)
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("ALL" to "सभी", "SUCCESS" to "पूर्ण", "FAILED" to "असफल").forEach { (key, label) ->
            FilterChip(
              selected = selectedFilter == key,
              onClick = { onSelectFilter(key) },
              label = { Text(label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = DeepVioletContainer,
                  selectedLabelColor = StarlightWhite,
                  containerColor = NebulaSurface,
                  labelColor = MistBlue,
                ),
              modifier = Modifier.testTag("history_filter_$key"),
            )
          }
        }
      }

      if (filteredHistory.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CosmicNavy,
            border = BorderStroke(1.dp, CosmicBorder),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MutedSlateBlue,
                modifier = Modifier.size(36.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "अभी कोई कमांड इतिहास नहीं है",
                style = MaterialTheme.typography.titleMedium,
                color = StarlightWhite,
              )
              Text(
                text = "सहायक टैब से हिंदी वॉइस कमांड बोलें या ऊपर से कोई त्वरित एक्शन आज़माएँ।",
                style = MaterialTheme.typography.bodySmall,
                color = MistBlue,
              )
            }
          }
        }
      } else {
        items(filteredHistory, key = { it.id }) { entry ->
          VoiceHistoryRow(
            entry = entry,
            onRunAgain = { onRunCommand(entry.spokenText) },
            onDelete = { onDeleteHistoryItem(entry.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun LauncherActionCard(item: QuickLauncherItem) {
  Surface(
    onClick = item.onClick,
    shape = RoundedCornerShape(18.dp),
    color = CosmicNavy,
    border = BorderStroke(1.dp, item.accent.copy(alpha = 0.5f)),
    tonalElevation = 3.dp,
    modifier = Modifier.width(146.dp).testTag("launcher_${item.titleHindi}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = item.icon,
          contentDescription = item.titleHindi,
          tint = item.accent,
          modifier = Modifier.size(26.dp),
        )
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          tint = MistBlue.copy(alpha = 0.7f),
          modifier = Modifier.size(16.dp),
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = item.titleHindi,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = StarlightWhite,
      )
      Text(
        text = item.subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MistBlue,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun VoiceHistoryRow(
  entry: VoiceCommandHistoryEntity,
  onRunAgain: () -> Unit,
  onDelete: () -> Unit,
) {
  val status =
    runCatching { CommandExecutionStatus.valueOf(entry.executionStatus) }
      .getOrDefault(CommandExecutionStatus.FAILED)

  val (badgeBg, badgeFg, badgeIcon, badgeText) =
    when (status) {
      CommandExecutionStatus.SUCCESS ->
        listOf(EmeraldContainer, EmeraldGlow, Icons.Filled.CheckCircle, "पूर्ण")
      CommandExecutionStatus.FAILED ->
        listOf(CoralContainer, CoralAlert, Icons.Filled.ErrorOutline, "असमर्थ")
      CommandExecutionStatus.PERMISSION_DENIED ->
        listOf(AmberContainer, AmberPulse, Icons.Filled.Shield, "अनुमति आवश्यक")
    }

  val formattedTime =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(entry.timestamp))

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = NebulaSurface,
    border = BorderStroke(1.dp, CosmicBorder),
  ) {
    Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = badgeBg as Color,
          shape = RoundedCornerShape(999.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = badgeIcon as ImageVector,
              contentDescription = null,
              tint = badgeFg as Color,
              modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = badgeText as String,
              style = MaterialTheme.typography.labelSmall,
              color = badgeFg,
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = entry.category,
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall,
          color = MutedSlateBlue,
          modifier = Modifier.padding(end = 8.dp),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = entry.spokenText,
            style = MaterialTheme.typography.titleMedium,
            color = StarlightWhite,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = entry.detailMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MistBlue,
            maxLines = 2,
          )
        }

        if (!entry.spokenText.startsWith("(")) {
          IconButton(
            onClick = onRunAgain,
            modifier = Modifier.size(48.dp).testTag("rerun_history_${entry.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Replay,
              contentDescription = "कमांड फिर चलाएँ",
              tint = CyberCyan,
            )
          }
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(48.dp).testTag("delete_history_${entry.id}"),
        ) {
          Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "इतिहास से हटाएँ",
            tint = CoralAlert.copy(alpha = 0.85f),
          )
        }
      }
    }
  }
}
