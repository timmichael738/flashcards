package com.kotlinapps.flashcards.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kotlinapps.flashcards.feature.decks.navigation.DecksDestination
import com.kotlinapps.flashcards.feature.decks.navigation.decksScreen
import com.kotlinapps.flashcards.feature.quiz.navigation.navigateToQuiz
import com.kotlinapps.flashcards.feature.quiz.navigation.quizScreen
import com.kotlinapps.flashcards.feature.settings.navigation.navigateToSettings
import com.kotlinapps.flashcards.feature.settings.navigation.settingsScreen
import com.kotlinapps.flashcards.feature.study.navigation.navigateToStudy
import com.kotlinapps.flashcards.feature.study.navigation.studyScreen

@Composable
fun FlashcardsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = DecksDestination,
        modifier = modifier,
    ) {
        decksScreen(
            onStudy = navController::navigateToStudy,
            onQuiz = navController::navigateToQuiz,
            onOpenSettings = navController::navigateToSettings,
        )
        studyScreen(onNavigateUp = navController::navigateUp)
        quizScreen(onNavigateUp = navController::navigateUp)
        settingsScreen(onNavigateUp = navController::navigateUp)
    }
}
