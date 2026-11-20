package com.kotlinapps.flashcards.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.kotlinapps.flashcards.core.model.BestScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

interface ScoreRepository {
    /** Best score per deck id; decks never played are absent. */
    val bestScores: Flow<Map<String, BestScore>>

    /** Saves [result] if it beats the stored best for [deckId]. Returns true when it is a new best. */
    suspend fun recordResult(deckId: String, result: BestScore): Boolean
}

internal class DataStoreScoreRepository(
    private val dataStore: DataStore<Preferences>,
) : ScoreRepository {

    private fun scoreKey(deckId: String) = intPreferencesKey(PREFIX + deckId + SCORE)
    private fun correctKey(deckId: String) = intPreferencesKey(PREFIX + deckId + CORRECT)
    private fun totalKey(deckId: String) = intPreferencesKey(PREFIX + deckId + TOTAL)

    override val bestScores: Flow<Map<String, BestScore>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs.asMap().keys
                .map { it.name }
                .filter { it.startsWith(PREFIX) && it.endsWith(SCORE) }
                .mapNotNull { name ->
                    val deckId = name.removePrefix(PREFIX).removeSuffix(SCORE)
                    prefs.readBest(deckId)?.let { deckId to it }
                }
                .toMap()
        }

    private fun Preferences.readBest(deckId: String): BestScore? {
        val score = this[scoreKey(deckId)] ?: return null
        return BestScore(score = score, correct = this[correctKey(deckId)] ?: 0, total = this[totalKey(deckId)] ?: 0)
    }

    override suspend fun recordResult(deckId: String, result: BestScore): Boolean {
        var isNewBest = false
        // Compare inside the edit transaction, so concurrent saves never overwrite a higher score.
        dataStore.edit { prefs ->
            val current = prefs.readBest(deckId)
            if (current == null || result.score > current.score) {
                prefs[scoreKey(deckId)] = result.score
                prefs[correctKey(deckId)] = result.correct
                prefs[totalKey(deckId)] = result.total
                isNewBest = true
            }
        }
        return isNewBest
    }

    private companion object {
        const val PREFIX = "best_"
        const val SCORE = "_score"
        const val CORRECT = "_correct"
        const val TOTAL = "_total"
    }
}
