package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = MintPrimary,
  onPrimary = Color(0xFF04211C),
  primaryContainer = MintDeep,
  onPrimaryContainer = Color.White,
  secondary = SkyInfo,
  onSecondary = Color(0xFF0B1E3A),
  tertiary = VioletHero,
  background = NightBg,
  onBackground = MiningTextPrimary,
  surface = NightSurface,
  onSurface = MiningTextPrimary,
  surfaceVariant = NightSurfaceHigh,
  onSurfaceVariant = MiningTextSecondary,
  outline = NightOutline,
  outlineVariant = NightSurfaceHigh,
  error = CoralWarn,
  onError = Color(0xFF3D0D16),
)

private val LightColorScheme = lightColorScheme(
  primary = MintDeep,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFB6F5EB),
  onPrimaryContainer = Color(0xFF04211C),
  secondary = Color(0xFF2563EB),
  onSecondary = Color.White,
  tertiary = VioletHero,
  background = Color(0xFFF4F7FB),
  onBackground = Color(0xFF101828),
  surface = Color.White,
  onSurface = Color(0xFF101828),
  surfaceVariant = Color(0xFFEAF0F7),
  onSurfaceVariant = Color(0xFF5B6B82),
  outline = Color(0xFFCBD5E4),
  error = Color(0xFFE11D48),
  onError = Color.White,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default gelap: kontras tinggi di area tambang & hemat baterai AMOLED
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}

