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

private val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    secondary = GoldGlow,
    tertiary = LightGold,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF003922),
    onSecondary = Color(0xFF3E2F00),
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = Color(0xFF1E2E29),
    onSurfaceVariant = TextLightSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    secondary = MetallicGold,
    tertiary = LightGold,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceAccent,
    onSurfaceVariant = TextDarkSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Forced to false so app always stays in beautiful bright light mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
