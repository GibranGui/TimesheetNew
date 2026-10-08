package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = MiningAmber,
  onPrimary = Color.Black,
  primaryContainer = MiningAmberDark,
  onPrimaryContainer = Color.White,
  secondary = MiningAccentBlue,
  onSecondary = Color.Black,
  background = MiningSlateBg,
  onBackground = MiningTextPrimary,
  surface = MiningSurface,
  onSurface = MiningTextPrimary,
  surfaceVariant = MiningSurfaceLight,
  onSurfaceVariant = MiningTextSecondary,
  error = MiningRedAlert,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = MiningAmberDark,
  onPrimary = Color.White,
  primaryContainer = MiningAmber,
  onPrimaryContainer = Color.Black,
  secondary = MiningAccentBlue,
  onSecondary = Color.Black,
  background = Color(0xFFF1F5F9),
  onBackground = Color(0xFF0F172A),
  surface = Color.White,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = Color(0xFF475569),
  error = MiningRedAlert,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to dark for high outdoor contrast and battery saving
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

