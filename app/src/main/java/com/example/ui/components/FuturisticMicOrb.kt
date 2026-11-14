package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ListeningState
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarlightWhite

@Composable
fun FuturisticMicOrb(
  listeningState: ListeningState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "mic_orb_transition")

  val pulsePhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pulse_phase",
    )

  val orbitAngle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 3600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "orbit_angle",
    )

  val rmsNormalized =
    when (listeningState) {
      is ListeningState.Listening -> ((listeningState.rmsDb + 2f) / 12f).coerceIn(0f, 1f)
      is ListeningState.Speaking -> 0.55f
      is ListeningState.Processing -> 0.35f
      ListeningState.Idle -> 0f
    }

  val animatedRms by
    animateFloatAsState(
      targetValue = rmsNormalized,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "animated_rms",
    )

  val orbScale by
    animateFloatAsState(
      targetValue =
        when (listeningState) {
          is ListeningState.Listening -> 1.06f + (animatedRms * 0.12f)
          is ListeningState.Speaking -> 1.04f
          is ListeningState.Processing -> 0.96f
          ListeningState.Idle -> 1.0f
        },
      animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
      label = "orb_scale",
    )

  val primaryGlow by
    animateColorAsState(
      targetValue =
        when (listeningState) {
          is ListeningState.Listening -> CyberCyan
          is ListeningState.Speaking -> AuroraMagenta
          is ListeningState.Processing -> NeonPurple
          ListeningState.Idle -> ElectricViolet
        },
      label = "primary_glow",
    )

  val secondaryGlow by
    animateColorAsState(
      targetValue =
        when (listeningState) {
          is ListeningState.Listening -> NeonPurple
          is ListeningState.Speaking -> CyberCyan
          else -> CyberCyan
        },
      label = "secondary_glow",
    )

  val statusLabel =
    when (listeningState) {
      is ListeningState.Idle -> "बोलने के लिए माइक दबाएँ"
      is ListeningState.Listening ->
        if (listeningState.partialText.isNotBlank()) {
          "\"${listeningState.partialText}\""
        } else {
          "सुन रहा हूँ… हिंदी में बोलें"
        }
      is ListeningState.Processing -> "समझ रहा हूँ…"
      is ListeningState.Speaking -> "बोल रहा हूँ… (रोकने के लिए दबाएँ)"
    }

  val contentDesc =
    when (listeningState) {
      is ListeningState.Idle -> stringResource(R.string.mic_listen_desc)
      is ListeningState.Listening -> stringResource(R.string.mic_stop_desc)
      is ListeningState.Speaking -> stringResource(R.string.mic_speaking_desc)
      is ListeningState.Processing -> "Processing voice command"
    }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier.size(128.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.32f

        // Ambient radial aura
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  primaryGlow.copy(alpha = if (listeningState is ListeningState.Idle) 0.25f else 0.45f),
                  secondaryGlow.copy(alpha = 0.14f),
                  Color.Transparent,
                ),
              center = center,
              radius = size.minDimension * 0.5f,
            ),
          radius = size.minDimension * 0.5f,
          center = center,
        )

        // Expanding sonar rings when active
        if (listeningState !is ListeningState.Idle) {
          val ring1Radius = baseRadius + (size.minDimension * 0.17f) * pulsePhase + (animatedRms * 10f)
          val ring2Phase = (pulsePhase + 0.5f) % 1f
          val ring2Radius = baseRadius + (size.minDimension * 0.17f) * ring2Phase

          drawCircle(
            color = primaryGlow.copy(alpha = (1f - pulsePhase) * 0.65f),
            radius = ring1Radius,
            center = center,
            style = Stroke(width = 3.5f),
          )
          drawCircle(
            color = secondaryGlow.copy(alpha = (1f - ring2Phase) * 0.5f),
            radius = ring2Radius,
            center = center,
            style = Stroke(width = 2.5f),
          )
        }

        // Rotating futuristic orbital arc
        drawArc(
          brush = Brush.sweepGradient(listOf(Color.Transparent, primaryGlow, secondaryGlow, Color.Transparent)),
          startAngle = orbitAngle,
          sweepAngle = if (listeningState is ListeningState.Idle) 110f else 220f,
          useCenter = false,
          topLeft = Offset(center.x - baseRadius * 1.24f, center.y - baseRadius * 1.24f),
          size = androidx.compose.ui.geometry.Size(baseRadius * 2.48f, baseRadius * 2.48f),
          style = Stroke(width = 4.5f, cap = StrokeCap.Round),
        )
      }

      val interactionSource = remember { MutableInteractionSource() }
      Box(
        modifier =
          Modifier
            .size(80.dp)
            .scale(orbScale)
            .clip(CircleShape)
            .background(
              brush =
                Brush.linearGradient(
                  colors =
                    when (listeningState) {
                      is ListeningState.Listening -> listOf(CyberCyan, ElectricViolet)
                      is ListeningState.Speaking -> listOf(AuroraMagenta, ElectricViolet)
                      else -> listOf(NeonPurple, CosmicNavy, ElectricViolet)
                    }
                )
            )
            .border(
              width = 2.dp,
              brush = Brush.linearGradient(listOf(StarlightWhite.copy(alpha = 0.7f), primaryGlow)),
              shape = CircleShape,
            )
            .clickable(
              interactionSource = interactionSource,
              indication = ripple(bounded = true, color = StarlightWhite),
              onClick = onClick,
            )
            .testTag("mic_button"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector =
            when (listeningState) {
              is ListeningState.Idle -> Icons.Filled.Mic
              is ListeningState.Listening -> Icons.Filled.Stop
              is ListeningState.Processing -> Icons.Filled.GraphicEq
              is ListeningState.Speaking -> Icons.AutoMirrored.Filled.VolumeUp
            },
          contentDescription = contentDesc,
          tint = StarlightWhite,
          modifier = Modifier.size(34.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = statusLabel,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color =
        when (listeningState) {
          is ListeningState.Listening -> CyberCyan
          is ListeningState.Speaking -> AuroraMagenta
          else -> StarlightWhite.copy(alpha = 0.9f)
        },
      textAlign = TextAlign.Center,
      maxLines = 2,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
  }
}
