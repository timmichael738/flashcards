package com.kotlinapps.flashcards.feature.decks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kotlinapps.flashcards.feature.decks.DecksRoute
import kotlinx.serialization.Serializable

@Serializable
data object DecksDestination

fun NavGraphBuilder.decksScreen(
    onStudy: (deckId: String) -> Unit,
    onQuiz: (deckId: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<DecksDestination> {
        DecksRoute(onStudy = onStudy, onQuiz = onQuiz, onOpenSettings = onOpenSettings)
    }
}
