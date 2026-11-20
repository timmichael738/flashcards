package com.kotlinapps.flashcards.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kotlinapps.flashcards.core.model.ThemeMode
import com.kotlinapps.flashcards.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(useDynamicColor: Boolean)

    suspend fun setQuestionsPerQuiz(count: Int)
}

internal class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val QUESTIONS_PER_QUIZ = intPreferencesKey("questions_per_quiz")
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        // A corrupted/unreadable file falls back to defaults instead of crashing the app.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val defaults = UserPreferences()
            UserPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.themeMode,
                useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                questionsPerQuiz = prefs[Keys.QUESTIONS_PER_QUIZ]
                    ?.takeIf { it in UserPreferences.QuestionsPerQuizOptions }
                    ?: defaults.questionsPerQuiz,
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamicColor }
    }

    override suspend fun setQuestionsPerQuiz(count: Int) {
        require(count in UserPreferences.QuestionsPerQuizOptions) { "Unsupported question count $count" }
        dataStore.edit { it[Keys.QUESTIONS_PER_QUIZ] = count }
    }
}
