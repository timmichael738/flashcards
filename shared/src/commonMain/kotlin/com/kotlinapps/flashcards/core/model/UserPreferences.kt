package com.kotlinapps.flashcards.core.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val questionsPerQuiz: Int = DEFAULT_QUESTIONS_PER_QUIZ,
) {
    companion object {
        const val DEFAULT_QUESTIONS_PER_QUIZ = 10
        val QuestionsPerQuizOptions = listOf(5, 10, 15)
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
