package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.domain.ListeningState
import com.example.system.DeviceStatusSnapshot
import com.example.system.VoiceEngineCapabilities
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.FuturisticMicOrb
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSpaceBlue
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MistBlue
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite

@Composable
fun AssistantScreen(
  messages: List<ChatMessageEntity>,
  listeningState: ListeningState,
  deviceStatus: DeviceStatusSnapshot,
  capabilities: VoiceEngineCapabilities,
  micGranted: Boolean,
  onMicClick: () -> Unit,
  onSendTypedCommand: (String) -> Unit,
  onQuickCommand: (String) -> Unit,
  onReplayAudio: (String) -> Unit,
  onRequestMicPermission: () -> Unit,
  onClearChat: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var inputText by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          brush =
            Brush.verticalGradient(
              colors = listOf(MidnightVoid, DeepSpaceBlue, Color(0xFF100C2B), MidnightVoid)
            )
        ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
      // Top Futuristic Header
      AssistantTopHeader(
        deviceStatus = deviceStatus,
        micGranted = micGranted,
        hasMessages = messages.isNotEmpty(),
        onRequestMicPermission = onRequestMicPermission,
        onClearChat = onClearChat,
      )

      // Capability Warning Banner if SpeechRecognizer is missing or Hindi TTS missing
      if (!capabilities.speechRecognizerAvailable || (capabilities.ttsInitialized && !capabilities.hindiTtsSupported)) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          color = AmberContainer.copy(alpha = 0.7f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, AmberPulse.copy(alpha = 0.55f)),
        ) {
          Text(
            text =
              if (!capabilities.speechRecognizerAvailable) {
                "वॉइस रिकग्निशन सेवा इस डिवाइस पर उपलब्ध नहीं है — आप नीचे हिंदी/Hinglish लिखकर कमांड चला सकते हैं।"
              } else {
                capabilities.ttsStatusNote
              },
            style = MaterialTheme.typography.bodySmall,
            color = StarlightWhite,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }

      // Chat Conversation Area
      if (messages.isEmpty()) {
        WelcomeEmptyState(
          onQuickCommand = onQuickCommand,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("chat_history_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(messages, key = { it.id }) { msg ->
            ChatMessageBubble(
              message = msg,
              onReplayAudio = onReplayAudio,
            )
          }
        }
      }

      // Quick Hindi Prompt Chips
      QuickPromptStrip(onQuickCommand = onQuickCommand)

      // Bottom Futuristic Voice & Text Dock
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CosmicNavy.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, CosmicBorder.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Typed Command Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it },
              modifier = Modifier.weight(1f).testTag("command_text_input"),
              placeholder = {
                Text(
                  text = stringResource(R.string.input_hint),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MistBlue.copy(alpha = 0.65f),
                )
              },
              singleLine = true,
              shape = RoundedCornerShape(20.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = CyberCyan,
                  unfocusedBorderColor = CosmicBorder,
                  focusedContainerColor = NebulaSurface,
                  unfocusedContainerColor = DeepSpaceBlue,
                  focusedTextColor = StarlightWhite,
                  unfocusedTextColor = StarlightWhite,
                  cursorColor = CyberCyan,
                ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions =
                KeyboardActions(
                  onSend = {
                    if (inputText.isNotBlank()) {
                      onSendTypedCommand(inputText)
                      inputText = ""
                    }
                  }
                ),
            )

            Surface(
              color = if (inputText.isNotBlank()) NeonPurple else NebulaSurface,
              shape = CircleShape,
              border = BorderStroke(1.dp, if (inputText.isNotBlank()) CyberCyan else CosmicBorder),
            ) {
              IconButton(
                onClick = {
                  if (inputText.isNotBlank()) {
                    onSendTypedCommand(inputText)
                    inputText = ""
                  }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier.size(48.dp).testTag("send_command_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = stringResource(R.string.send_desc),
                  tint = if (inputText.isNotBlank()) MidnightVoid else MistBlue.copy(alpha = 0.5f),
                )
              }
            }
          }

          // Large Animated Microphone Orb
          FuturisticMicOrb(
            listeningState = listeningState,
            onClick = onMicClick,
          )
        }
      }
    }
  }
}

@Composable
private fun AssistantTopHeader(
  deviceStatus: DeviceStatusSnapshot,
  micGranted: Boolean,
  hasMessages: Boolean,
  onRequestMicPermission: () -> Unit,
  onClearChat: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    shape = RoundedCornerShape(22.dp),
    color = CosmicNavy.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, CosmicBorder),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Image(
          painter = painterResource(id = R.drawable.durgesh_ai_icon_1794658792977),
          contentDescription = "Durgesh AI emblem",
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Durgesh AI",
            style = MaterialTheme.typography.titleLarge,
            color = StarlightWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "POCO HyperOS हिंदी वॉइस असिस्टेंट",
            style = MaterialTheme.typography.labelMedium,
            color = CyberCyan,
          )
        }

        if (!micGranted) {
          Surface(
            onClick = onRequestMicPermission,
            shape = RoundedCornerShape(12.dp),
            color = AmberContainer,
            border = BorderStroke(1.dp, AmberPulse),
            modifier = Modifier.testTag("mic_permission_chip"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = "माइक अनुमति दें",
                tint = AmberPulse,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "माइक अनुमति",
                style = MaterialTheme.typography.labelSmall,
                color = AmberPulse,
              )
            }
          }
        }

        if (hasMessages) {
          IconButton(
            onClick = onClearChat,
            modifier = Modifier.size(48.dp).testTag("clear_chat_icon_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteSweep,
              contentDescription = "चैट साफ़ करें",
              tint = MistBlue,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Real Device Telemetry Pills
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TelemetryChip(
          icon = Icons.Filled.PhoneAndroid,
          label = "${deviceStatus.deviceModelLabel} • ${deviceStatus.osVersionLabel}",
          tint = NeonPurple,
        )
        if (deviceStatus.batteryPercent >= 0) {
          TelemetryChip(
            icon =
              if (deviceStatus.isCharging) Icons.Filled.BatteryChargingFull
              else Icons.Filled.BatteryStd,
            label = "${deviceStatus.batteryPercent}%",
            tint = EmeraldGlow,
          )
        }
        TelemetryChip(
          icon = if (deviceStatus.isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
          label = deviceStatus.networkTypeLabel,
          tint = if (deviceStatus.isOnline) CyberCyan else AmberPulse,
        )
      }
    }
  }
}

@Composable
private fun TelemetryChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
) {
  Surface(
    color = NebulaSurface,
    shape = RoundedCornerShape(999.dp),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = StarlightWhite)
    }
  }
}

@Composable
private fun WelcomeEmptyState(
  onQuickCommand: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = CosmicNavy.copy(alpha = 0.8f),
      border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f)),
      tonalElevation = 4.dp,
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(ElectricViolet, CyberCyan))),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            tint = StarlightWhite,
            modifier = Modifier.size(30.dp),
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "नमस्ते! मैं Durgesh AI हूँ",
          style = MaterialTheme.typography.headlineSmall,
          color = StarlightWhite,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text =
            "नीचे दिए बड़े माइक बटन को दबाकर हिंदी में बोलें — ऐप्स खोलें, Wi-Fi / Bluetooth सेटिंग्स देखें, या ऑफ़लाइन कमांड चलाएँ।",
          style = MaterialTheme.typography.bodyMedium,
          color = MistBlue,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("यूट्यूब खोलो", "बैटरी कितनी है", "वाई-फ़ाई सेटिंग्स", "समय क्या हुआ है").forEach { sample ->
            Surface(
              onClick = { onQuickCommand(sample) },
              color = DeepVioletContainer,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
            ) {
              Text(
                text = sample,
                style = MaterialTheme.typography.labelLarge,
                color = StarlightWhite,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickPromptStrip(onQuickCommand: (String) -> Unit) {
  val prompts =
    listOf(
      "यूट्यूब खोलो",
      "व्हाट्सएप खोलो",
      "क्रोम खोलो",
      "कैमरा खोलो",
      "वाई-फ़ाई सेटिंग्स",
      "ब्लूटूथ सेटिंग्स",
      "बैटरी कितनी है",
      "टॉर्च जलाओ",
      "समय क्या हुआ है",
    )

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    prompts.forEach { cmd ->
      AssistChip(
        onClick = { onQuickCommand(cmd) },
        label = { Text(text = cmd, style = MaterialTheme.typography.labelMedium) },
        colors =
          AssistChipDefaults.assistChipColors(
            containerColor = NebulaSurface,
            labelColor = StarlightWhite,
          ),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("quick_chip_$cmd"),
      )
    }
  }
}
