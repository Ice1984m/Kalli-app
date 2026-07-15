package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = KaliPrimary,
    secondary = KaliSecondary,
    tertiary = KaliTertiary,
    background = KaliBackground,
    surface = KaliSurface,
    surfaceVariant = KaliSurfaceVariant,
    onPrimary = Color(0xFF001F08),
    onSecondary = Color(0xFF001F24),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    error = KaliError
  )

private val LightColorScheme = DarkColorScheme // Keep it dark for that authentic Kali Hacker theme!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
