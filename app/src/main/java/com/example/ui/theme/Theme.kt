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

private val AppColorScheme = darkColorScheme(
    primary = RoyalGold,
    secondary = GoldGlow,
    tertiary = LightGold,
    background = DeepObsidian,
    surface = IslamicGreenSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = IslamicGreenCardBg,
    onSurfaceVariant = TextLightSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

