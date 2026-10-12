package com.ttggg5.holovoice.ui.theme

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

private val HoloBlue = Color(0xFF1E9BD7)
private val HoloBlueLight = Color(0xFF7FD3F7)
private val Accent = Color(0xFFF06292)

private val LightColors = lightColorScheme(
    primary = HoloBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EEFB),
    onPrimaryContainer = Color(0xFF00344D),
    secondary = Accent,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = HoloBlueLight,
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004C6E),
    onPrimaryContainer = Color(0xFFD3EEFB),
    secondary = Color(0xFFF8A5C2),
    onSecondary = Color(0xFF4A0F25),
)

@Composable
fun HoloVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
