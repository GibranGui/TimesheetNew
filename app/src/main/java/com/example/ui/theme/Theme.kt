package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EmployeePrimary,
    onPrimary = EmployeeOnPrimary,
    primaryContainer = EmployeePrimarySoft,
    onPrimaryContainer = Color(0xFF0B2A45),
    secondary = EmployeeAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8CC),
    onSecondaryContainer = Color(0xFF6B3A05),
    tertiary = EmployeeViolet,
    background = EmployeeBg,
    onBackground = EmployeeTextMain,
    surface = EmployeeSurface,
    onSurface = EmployeeTextMain,
    surfaceVariant = EmployeeSurfaceSoft,
    onSurfaceVariant = EmployeeTextSub,
    outline = EmployeeOutline,
    outlineVariant = EmployeeOutline,
    error = EmployeeDangerRed,
    onError = Color.White,
)

// Skema gelap tetap tersedia (mis. mode gelap sistem) dengan warna
// roaster yang dilembutkan agar tetap terbaca di latar gelap.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7FB4E6),
    onPrimary = Color(0xFF0B2A45),
    primaryContainer = Color(0xFF173A58),
    onPrimaryContainer = Color(0xFFDCE9F6),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3A2205),
    background = Color(0xFF10151C),
    onBackground = Color(0xFFE7ECF3),
    surface = Color(0xFF171E28),
    onSurface = Color(0xFFE7ECF3),
    surfaceVariant = Color(0xFF22303F),
    onSurfaceVariant = Color(0xFFA9B6C6),
    outline = Color(0xFF3C4A5C),
    error = Color(0xFFF08A8A),
    onError = Color(0xFF3D0D0D),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Tema utama "karyawan": terang & bersih
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
