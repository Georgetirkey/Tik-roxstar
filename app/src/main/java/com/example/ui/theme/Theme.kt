package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TikRoxAccentPink,
    secondary = TikRoxAccentCyan,
    tertiary = TikRoxWhite,
    background = TikRoxBlack,
    surface = TikRoxDarkGray,
    onPrimary = TikRoxWhite,
    onSecondary = TikRoxBlack,
    onTertiary = TikRoxBlack,
    onBackground = TikRoxWhite,
    onSurface = TikRoxWhite,
    surfaceVariant = TikRoxLightGray,
    onSurfaceVariant = TikRoxGrayText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark aesthetic by default for TikTok immersive experience
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve Tik Rox's signature neon brand theme
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
