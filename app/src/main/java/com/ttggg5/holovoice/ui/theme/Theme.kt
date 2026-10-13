package com.ttggg5.holovoice.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.Accent
import com.ttggg5.holovoice.data.ThemeMode

/** Colours for one accent: strong/soft primary and secondary for light and dark themes. */
private data class AccentColors(
    val light: Color,
    val lightContainer: Color,
    val onLightContainer: Color,
    val dark: Color,
    val darkContainer: Color,
    val onDarkContainer: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
)

private fun Accent.colors(): AccentColors = when (this) {
    Accent.HOLO_BLUE -> AccentColors(
        Color(0xFF1E9BD7), Color(0xFFD3EEFB), Color(0xFF00344D),
        Color(0xFF7FD3F7), Color(0xFF004C6E), Color(0xFFD3EEFB),
        Color(0xFFF06292), Color(0xFFF8A5C2),
    )
    Accent.SAKURA_PINK -> AccentColors(
        Color(0xFFD9467F), Color(0xFFFFD9E4), Color(0xFF3E001D),
        Color(0xFFFFB0CB), Color(0xFF7A1F47), Color(0xFFFFD9E4),
        Color(0xFF1E9BD7), Color(0xFF7FD3F7),
    )
    Accent.LAVENDER -> AccentColors(
        Color(0xFF7B5CC4), Color(0xFFE9DDFF), Color(0xFF23005C),
        Color(0xFFCFBCFF), Color(0xFF4F378B), Color(0xFFE9DDFF),
        Color(0xFFE5739B), Color(0xFFF8A5C2),
    )
    Accent.MINT -> AccentColors(
        Color(0xFF138A72), Color(0xFFB8F2E1), Color(0xFF00201A),
        Color(0xFF7FDBC2), Color(0xFF005142), Color(0xFFB8F2E1),
        Color(0xFF6B7CD9), Color(0xFFB7C3FF),
    )
    Accent.SUNSET -> AccentColors(
        Color(0xFFE0662E), Color(0xFFFFDBCC), Color(0xFF370E00),
        Color(0xFFFFB693), Color(0xFF7B2F0C), Color(0xFFFFDBCC),
        Color(0xFF9C5CC4), Color(0xFFDDB8FF),
    )
}

fun accentSwatch(accent: Accent): Color = accent.colors().light

private fun scheme(accent: Accent, dark: Boolean): ColorScheme {
    val c = accent.colors()
    return if (dark) {
        darkColorScheme(
            primary = c.dark,
            onPrimary = c.onLightContainer,
            primaryContainer = c.darkContainer,
            onPrimaryContainer = c.onDarkContainer,
            secondary = c.secondaryDark,
            onSecondary = Color(0xFF2B0A18),
        )
    } else {
        lightColorScheme(
            primary = c.light,
            onPrimary = Color.White,
            primaryContainer = c.lightContainer,
            onPrimaryContainer = c.onLightContainer,
            secondary = c.secondaryLight,
            onSecondary = Color.White,
        )
    }
}

/**
 * App theme, following the theme mode, Material You and accent chosen in Settings. With
 * "Match the playing talent" on, the main colours come from the talent who is speaking.
 */
@Composable
fun HoloVoiceTheme(content: @Composable () -> Unit) {
    val prefs = LocalContext.current.appContainer.uiPrefs
    val mode by prefs.themeMode.state.collectAsStateWithLifecycle()
    val dynamic by prefs.dynamicColor.state.collectAsStateWithLifecycle()
    val accent by prefs.accent.state.collectAsStateWithLifecycle()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val base = if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        scheme(accent, dark)
    }

    // While a clip is loaded, take the main colours from the speaking talent's picture.
    val followTalent by prefs.talentColor.state.collectAsStateWithLifecycle()
    val playerState by context.appContainer.player.state.collectAsStateWithLifecycle()
    val talentImage = playerState.talentImageUrl.takeIf { followTalent }
    val seed by produceState<Color?>(null, talentImage) {
        value = talentImage?.let { runCatching { TalentColor.seed(context, it) }.getOrNull() }
    }
    val target = seed?.let { TalentColor.scheme(it, dark) } ?: base

    // Fade between talents instead of jumping.
    val spec = tween<Color>(600)
    val colors = base.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, spec, label = "onSecondary").value,
    )
    MaterialTheme(colorScheme = colors, content = content)
}
