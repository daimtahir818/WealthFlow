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

private val DarkColorScheme = darkColorScheme(
    primary = NeonCoral,
    onPrimary = TextPrimary,
    primaryContainer = NeonCoralMuted,
    secondary = AccentBlue,
    onSecondary = TextPrimary,
    tertiary = HighIndigo,
    background = MidnightBackground,
    surface = DeepMidnightCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = SurfaceLine,
    surfaceVariant = HighIndigo,
    onSurfaceVariant = TextMuted
)

private val LightColorScheme = DarkColorScheme // Standardize on Midnight Premium aesthetic

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark/Midnight by default as requested
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve premium branding
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
