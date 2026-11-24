package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Datalabs Brand Palette — Deep Navy, Crisp Editorial White, Vibrant Blue Accent
val NavyDeep = Color(0xFF0B1B36)
val NavySurface = Color(0xFF0F2547)
val NavyElevated = Color(0xFF16325C)
val NavyMuted = Color(0xFF1E3A6E)

val ElectricBlue = Color(0xFF2563EB)
val ElectricBlueBright = Color(0xFF3B82F6)
val ElectricBlueSoft = Color(0xFFDBEAFE)
val ElectricBlueContainer = Color(0xFFEFF6FF)

val SlateInk = Color(0xFF0F172A)
val SlateBody = Color(0xFF334155)
val SlateSecondary = Color(0xFF64748B)
val SlateBorder = Color(0xFFE2E8F0)
val SlateSurfaceVariant = Color(0xFFF1F5F9)
val EditorialBackground = Color(0xFFF8FAFC)
val PureWhite = Color(0xFFFFFFFF)

val SuccessEmerald = Color(0xFF059669)
val SuccessContainer = Color(0xFFD1FAE5)
val WarningGold = Color(0xFFD97706)
val WarningContainer = Color(0xFFFEF3C7)
val ErrorCrimson = Color(0xFFDC2626)
val ErrorContainer = Color(0xFFFEE2E2)

// Dark Mode Tokens
val DarkBackground = Color(0xFF060E1D)
val DarkSurface = Color(0xFF0B172E)
val DarkSurfaceVariant = Color(0xFF132444)
val DarkBorder = Color(0xFF1E345E)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceSecondary = Color(0xFF94A3B8)

fun parseAccentColor(hex: String, fallback: Color = ElectricBlue): Color {
  return try {
    val clean = hex.trim().removePrefix("#")
    val colorInt =
      when (clean.length) {
        6 -> (0xFF000000L or clean.toLong(16)).toInt()
        8 -> clean.toLong(16).toInt()
        else -> return fallback
      }
    Color(colorInt)
  } catch (_: Exception) {
    fallback
  }
}
