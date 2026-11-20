package com.kotlinapps.flashcards.feature.study.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kotlinapps.flashcards.feature.study.StudyRoute
import kotlinx.serialization.Serializable

@Serializable
data class StudyDestination(val deckId: String)

fun NavController.navigateToStudy(deckId: String) = navigate(StudyDestination(deckId))

fun NavGraphBuilder.studyScreen(onNavigateUp: () -> Unit) {
    composable<StudyDestination> { backStackEntry ->
        StudyRoute(deckId = backStackEntry.toRoute<StudyDestination>().deckId, onNavigateUp = onNavigateUp)
    }
}
