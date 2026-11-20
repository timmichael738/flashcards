package com.kotlinapps.flashcards

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.flashcards.core.designsystem.theme.FlashcardsTheme
import com.kotlinapps.flashcards.core.model.ThemeMode
import com.kotlinapps.flashcards.navigation.FlashcardsNavHost
import org.koin.compose.viewmodel.koinViewModel

/** Root composable shared by Android and iOS. Applies the user's saved theme preferences. */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    // Wait for stored preferences before drawing, so the wrong theme never flashes.
    val prefs = preferences ?: return

    val darkTheme = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    FlashcardsTheme(darkTheme = darkTheme, dynamicColor = prefs.useDynamicColor) {
        FlashcardsNavHost()
    }
}
