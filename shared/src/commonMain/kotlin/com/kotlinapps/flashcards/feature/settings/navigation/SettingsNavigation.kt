package com.kotlinapps.flashcards.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kotlinapps.flashcards.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

fun NavController.navigateToSettings() = navigate(SettingsDestination)

fun NavGraphBuilder.settingsScreen(onNavigateUp: () -> Unit) {
    composable<SettingsDestination> {
        SettingsRoute(onNavigateUp = onNavigateUp)
    }
}
