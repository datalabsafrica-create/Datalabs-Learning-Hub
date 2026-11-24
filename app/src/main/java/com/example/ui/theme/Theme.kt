package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = ElectricBlue,
  onPrimary = PureWhite,
  primaryContainer = ElectricBlueContainer,
  onPrimaryContainer = NavyDeep,
  secondary = NavyDeep,
  onSecondary = PureWhite,
  secondaryContainer = SlateSurfaceVariant,
  onSecondaryContainer = SlateInk,
  tertiary = WarningGold,
  onTertiary = PureWhite,
  tertiaryContainer = WarningContainer,
  onTertiaryContainer = SlateInk,
  background = EditorialBackground,
  onBackground = SlateInk,
  surface = PureWhite,
  onSurface = SlateInk,
  surfaceVariant = SlateSurfaceVariant,
  onSurfaceVariant = SlateSecondary,
  outline = SlateBorder,
  outlineVariant = SlateBorder,
  error = ErrorCrimson,
  onError = PureWhite,
  errorContainer = ErrorContainer,
  onErrorContainer = ErrorCrimson
)

private val DarkColorScheme = darkColorScheme(
  primary = ElectricBlueBright,
  onPrimary = NavyDeep,
  primaryContainer = NavyMuted,
  onPrimaryContainer = ElectricBlueSoft,
  secondary = ElectricBlueSoft,
  onSecondary = NavyDeep,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = DarkOnSurface,
  tertiary = WarningGold,
  onTertiary = NavyDeep,
  tertiaryContainer = DarkSurfaceVariant,
  onTertiaryContainer = WarningContainer,
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorder,
  error = ErrorCrimson,
  onError = PureWhite,
  errorContainer = DarkSurfaceVariant,
  onErrorContainer = ErrorContainer
)

val DatalabsShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DatalabsTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = DatalabsTypography,
    shapes = DatalabsShapes,
    content = content
  )
}
