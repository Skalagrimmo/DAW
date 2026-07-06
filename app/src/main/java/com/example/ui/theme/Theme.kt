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
    primary = NeonGreen,
    secondary = NeonOrange,
    tertiary = NeonBlue,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = LightWhite,
    onSurface = LightWhite,
    onPrimary = Color(0xFF381E72),
    onSecondary = DarkBg,
    outline = Color(0xFF44474E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for professional studio feel
  dynamicColor: Boolean = false, // Use our custom handcrafted colors instead of dynamic
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
