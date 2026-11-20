package com.kotlinapps.flashcards.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.kotlinapps.flashcards.core.model.ThemeMode
import com.kotlinapps.flashcards.core.model.UserPreferences
import com.kotlinapps.flashcards.testing.FakeUserPreferencesRepository
import com.kotlinapps.flashcards.testing.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsViewModelTest : MainDispatcherTest() {

    private val viewModel = SettingsViewModel(FakeUserPreferencesRepository())

    private suspend fun ReceiveTurbine<SettingsUiState>.awaitPreferences(): UserPreferences {
        var item = awaitItem()
        if (item is SettingsUiState.Loading) item = awaitItem()
        return (item as SettingsUiState.Success).preferences
    }

    @Test
    fun uiState_initiallyLoading() {
        assertEquals(SettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun changes_areReflectedInState() = runTest {
        viewModel.uiState.test {
            assertEquals(UserPreferences(), awaitPreferences())

            viewModel.onThemeModeChange(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitPreferences().themeMode)

            viewModel.onDynamicColorChange(false)
            assertEquals(false, awaitPreferences().useDynamicColor)

            viewModel.onQuestionsPerQuizChange(15)
            assertEquals(15, awaitPreferences().questionsPerQuiz)
        }
    }
}
