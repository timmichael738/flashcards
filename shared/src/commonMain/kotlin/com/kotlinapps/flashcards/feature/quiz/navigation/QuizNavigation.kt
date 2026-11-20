package com.kotlinapps.flashcards.feature.quiz.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kotlinapps.flashcards.feature.quiz.QuizRoute
import kotlinx.serialization.Serializable

@Serializable
data class QuizDestination(val deckId: String)

fun NavController.navigateToQuiz(deckId: String) = navigate(QuizDestination(deckId))

fun NavGraphBuilder.quizScreen(onNavigateUp: () -> Unit) {
    composable<QuizDestination> { backStackEntry ->
        QuizRoute(deckId = backStackEntry.toRoute<QuizDestination>().deckId, onNavigateUp = onNavigateUp)
    }
}
