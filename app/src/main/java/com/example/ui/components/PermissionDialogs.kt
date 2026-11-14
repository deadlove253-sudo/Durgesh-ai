package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.PermissionDialogState
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSpaceBlue
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MistBlue
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite

@Composable
fun PermissionRationaleDialog(
  state: PermissionDialogState,
  onAllowClick: () -> Unit,
  onOpenSettingsClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DeepSpaceBlue,
    titleContentColor = StarlightWhite,
    textContentColor = MistBlue,
    icon = {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = NebulaSurface,
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f)),
      ) {
        Icon(
          imageVector =
            if (state.permission == android.Manifest.permission.RECORD_AUDIO) {
              Icons.Filled.Mic
            } else {
              Icons.Filled.Security
            },
          contentDescription = null,
          tint = CyberCyan,
          modifier = Modifier.padding(12.dp).size(28.dp),
        )
      }
    },
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = state.titleHindi, style = MaterialTheme.typography.titleLarge)
        Text(
          text = state.titleEnglish,
          style = MaterialTheme.typography.labelMedium,
          color = CyberCyan,
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = state.explanationHindi,
          style = MaterialTheme.typography.bodyMedium,
          color = StarlightWhite,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = NebulaSurface,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, CosmicBorder),
        ) {
          Text(
            text = state.explanationEnglish,
            style = MaterialTheme.typography.bodySmall,
            color = MistBlue,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    },
    confirmButton = {
      if (state.isPermanentlyDenied) {
        Button(
          onClick = onOpenSettingsClick,
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightVoid),
          modifier = Modifier.testTag("open_app_settings_button"),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("App Settings खोलें")
          }
        }
      } else {
        Button(
          onClick = onAllowClick,
          colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = MidnightVoid),
          modifier = Modifier.testTag("grant_permission_button"),
        ) {
          Text("अनुमति दें (Allow)")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_permission_button")) {
        Text("बाद में (Cancel)", color = MistBlue)
      }
    },
  )
}
