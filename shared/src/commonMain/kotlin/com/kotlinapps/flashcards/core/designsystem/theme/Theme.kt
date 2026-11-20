package com.kotlinapps.flashcards.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3FB8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DEFF),
    onPrimaryContainer = Color(0xFF1C0062),
    secondary = Color(0xFF006A60),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9EF2E3),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF8E4E00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC0),
    onTertiaryContainer = Color(0xFF2D1600),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCCBDFF),
    onPrimary = Color(0xFF2D0A8C),
    primaryContainer = Color(0xFF43249E),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF82D5C7),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF9EF2E3),
    tertiary = Color(0xFFFFB877),
    onTertiary = Color(0xFF4C2700),
    tertiaryContainer = Color(0xFF6C3A00),
    onTertiaryContainer = Color(0xFFFFDCC0),
)

/** True when the platform supports Material You dynamic color (Android 12+ only). */
expect val supportsDynamicColor: Boolean

/** Wallpaper-based colors, or null where unsupported. */
@Composable
internal expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
fun FlashcardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = (if (dynamicColor) dynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColors else LightColors

    MaterialTheme(colorScheme = colorScheme, content = content)
}
