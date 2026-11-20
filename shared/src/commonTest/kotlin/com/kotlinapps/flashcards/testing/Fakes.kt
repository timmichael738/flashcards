package com.kotlinapps.flashcards.testing

import com.kotlinapps.flashcards.core.data.deck.DeckRepository
import com.kotlinapps.flashcards.core.data.repository.ScoreRepository
import com.kotlinapps.flashcards.core.data.repository.UserPreferencesRepository
import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.core.model.Deck
import com.kotlinapps.flashcards.core.model.DeckAccent
import com.kotlinapps.flashcards.core.model.Flashcard
import com.kotlinapps.flashcards.core.model.ThemeMode
import com.kotlinapps.flashcards.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

fun testDeck(id: String = "capitals", size: Int = 6) = Deck(
    id = id,
    title = "Deck $id",
    description = "",
    accent = DeckAccent.TEAL,
    cards = (1..size).map { Flashcard(front = "Q$it", back = "A$it") },
)

class FakeDeckRepository(var decks: List<Deck> = listOf(testDeck())) : DeckRepository {
    var failWith: Exception? = null
    var calls = 0
        private set

    override suspend fun getDecks(): List<Deck> {
        calls++
        failWith?.let { throw it }
        return decks
    }

    override suspend fun getDeck(id: String): Deck? = getDecks().firstOrNull { it.id == id }
}

class FakeScoreRepository(initial: Map<String, BestScore> = emptyMap()) : ScoreRepository {
    private val state = MutableStateFlow(initial)
    override val bestScores: Flow<Map<String, BestScore>> = state
    val recorded = mutableListOf<Pair<String, BestScore>>()

    override suspend fun recordResult(deckId: String, result: BestScore): Boolean {
        recorded += deckId to result
        val current = state.value[deckId]
        if (current != null && result.score <= current.score) return false
        state.update { it + (deckId to result) }
        return true
    }
}

class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        state.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun setQuestionsPerQuiz(count: Int) = state.update { it.copy(questionsPerQuiz = count) }
}
