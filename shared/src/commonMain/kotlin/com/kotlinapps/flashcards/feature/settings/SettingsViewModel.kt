package com.kotlinapps.flashcards.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.flashcards.core.data.repository.UserPreferencesRepository
import com.kotlinapps.flashcards.core.model.ThemeMode
import com.kotlinapps.flashcards.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val preferences: UserPreferences) : SettingsUiState
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = userPreferencesRepository.userPreferences
        .map<UserPreferences, SettingsUiState> { SettingsUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun onThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(themeMode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setUseDynamicColor(enabled) }
    }

    fun onQuestionsPerQuizChange(count: Int) {
        viewModelScope.launch { userPreferencesRepository.setQuestionsPerQuiz(count) }
    }
}
