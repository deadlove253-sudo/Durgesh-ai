package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.InputMode
import com.example.data.local.MessageStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepVioletContainer
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MistBlue
import com.example.ui.theme.MutedSlateBlue
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageBubble(
  message: ChatMessageEntity,
  onReplayAudio: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isUser = message.isFromUser
  val status =
    runCatching { MessageStatus.valueOf(message.status) }.getOrDefault(MessageStatus.INFO)
  val inputMode =
    runCatching { InputMode.valueOf(message.inputMode) }.getOrDefault(InputMode.VOICE)

  val timeLabel =
    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(message.timestamp))

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
  ) {
    if (isUser) {
      Surface(
        modifier = Modifier.widthIn(max = 310.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        color = DeepVioletContainer,
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.55f)),
        tonalElevation = 4.dp,
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector =
                if (inputMode == InputMode.VOICE) Icons.Filled.Mic else Icons.Filled.Keyboard,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (inputMode == InputMode.VOICE) "वॉइस कमांड" else "लिखा गया कमांड",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Text(
              text = timeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MistBlue.copy(alpha = 0.7f),
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = StarlightWhite,
          )
        }
      }
    } else {
      val borderColor =
        when (status) {
          MessageStatus.SUCCESS -> CyberCyan.copy(alpha = 0.55f)
          MessageStatus.CANNOT_PERFORM -> CoralAlert.copy(alpha = 0.6f)
          MessageStatus.PERMISSION_REQUIRED -> AmberPulse.copy(alpha = 0.65f)
          MessageStatus.INFO -> CosmicBorder
        }

      Surface(
        modifier = Modifier.widthIn(max = 330.dp),
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        color = NebulaSurface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 3.dp,
      ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier =
                Modifier
                  .background(
                    brush = Brush.linearGradient(listOf(ElectricViolet, CyberCyan)),
                    shape = RoundedCornerShape(8.dp),
                  )
                  .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = StarlightWhite,
                  modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Durgesh AI",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = StarlightWhite,
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(status = status, actionLabel = message.actionLabel)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
              onClick = { onReplayAudio(message.text) },
              modifier = Modifier.size(48.dp).testTag("replay_tts_${message.id}"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "हिंदी में फिर से सुनें",
                tint = CyberCyan,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = StarlightWhite,
            modifier = Modifier.padding(end = 8.dp),
          )

          if (!message.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = message.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MistBlue.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlateBlue,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatusBadge(status: MessageStatus, actionLabel: String?) {
  val (bgColor, fgColor, icon, label) =
    when (status) {
      MessageStatus.SUCCESS ->
        Quad(EmeraldContainer, EmeraldGlow, Icons.Filled.CheckCircle, actionLabel ?: "पूर्ण")
      MessageStatus.CANNOT_PERFORM ->
        Quad(CoralContainer, CoralAlert, Icons.Filled.ErrorOutline, "असमर्थ")
      MessageStatus.PERMISSION_REQUIRED ->
        Quad(AmberContainer, AmberPulse, Icons.Filled.Shield, "अनुमति")
      MessageStatus.INFO ->
        Quad(DeepCyanContainer, CyberCyan, Icons.Filled.AutoAwesome, actionLabel ?: "जानकारी")
    }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(999.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fgColor,
        modifier = Modifier.size(12.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fgColor,
        maxLines = 1,
      )
    }
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
