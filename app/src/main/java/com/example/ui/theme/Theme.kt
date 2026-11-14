package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DurgeshDarkColorScheme =
  darkColorScheme(
    primary = NeonPurple,
    onPrimary = MidnightVoid,
    primaryContainer = DeepVioletContainer,
    onPrimaryContainer = StarlightWhite,
    secondary = CyberCyan,
    onSecondary = MidnightVoid,
    secondaryContainer = DeepCyanContainer,
    onSecondaryContainer = StarlightWhite,
    tertiary = AuroraMagenta,
    onTertiary = MidnightVoid,
    background = MidnightVoid,
    onBackground = StarlightWhite,
    surface = DeepSpaceBlue,
    onSurface = StarlightWhite,
    surfaceVariant = NebulaSurface,
    onSurfaceVariant = MistBlue,
    surfaceContainerLowest = MidnightVoid,
    surfaceContainerLow = DeepSpaceBlue,
    surfaceContainer = CosmicNavy,
    surfaceContainerHigh = NebulaSurface,
    surfaceContainerHighest = NebulaSurfaceElevated,
    outline = CosmicBorder,
    outlineVariant = ColorTokens.SubtleBorder,
    error = CoralAlert,
    onError = MidnightVoid,
    errorContainer = CoralContainer,
    onErrorContainer = StarlightWhite,
  )

private object ColorTokens {
  val SubtleBorder = CosmicBorder.copy(alpha = 0.55f)
}

val DurgeshShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun DurgeshAITheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = DurgeshDarkColorScheme,
    typography = DurgeshTypography,
    shapes = DurgeshShapes,
    content = content,
  )
}
