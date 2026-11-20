package com.kotlinapps.flashcards.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.flashcards.core.data.deck.DeckRepository
import com.kotlinapps.flashcards.core.data.repository.ScoreRepository
import com.kotlinapps.flashcards.core.data.repository.UserPreferencesRepository
import com.kotlinapps.flashcards.core.domain.QuizGenerator
import com.kotlinapps.flashcards.core.domain.QuizSession
import com.kotlinapps.flashcards.core.domain.suspendRunCatching
import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.core.model.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object NotFound : QuizUiState
    data object Error : QuizUiState

    data class InProgress(val deckTitle: String, val session: QuizSession) : QuizUiState

    data class Finished(
        val deckTitle: String,
        val result: BestScore,
        val bestStreak: Int,
        val isNewBest: Boolean,
        /** Best score before this run, or null if the deck had never been played. */
        val previousBest: BestScore?,
    ) : QuizUiState {
        val accuracyPercent: Int get() = if (result.total == 0) 0 else result.correct * 100 / result.total
    }
}

class QuizViewModel(
    private val deckId: String,
    private val deckRepository: DeckRepository,
    private val scoreRepository: ScoreRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val random: Random,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var deck: Deck? = null
    private var previousBest: BestScore? = null

    init {
        startNewQuiz()
    }

    /** Loads the deck (first time only) and starts a fresh, reshuffled quiz. Also used for retry and "Play again". */
    fun startNewQuiz() {
        _uiState.value = QuizUiState.Loading
        viewModelScope.launch {
            _uiState.value = suspendRunCatching {
                val loaded = deck ?: deckRepository.getDeck(deckId)?.also { deck = it }
                if (loaded == null) {
                    QuizUiState.NotFound
                } else {
                    previousBest = scoreRepository.bestScores.first()[deckId]
                    val count = preferencesRepository.userPreferences.first().questionsPerQuiz
                    val questions = QuizGenerator.generate(loaded.cards, count, random)
                    QuizUiState.InProgress(loaded.title, QuizSession(questions))
                }
            }.getOrElse { QuizUiState.Error }
        }
    }

    fun onAnswer(optionIndex: Int) {
        val state = _uiState.value as? QuizUiState.InProgress ?: return
        _uiState.value = state.copy(session = state.session.answer(optionIndex))
    }

    fun onNext() {
        val state = _uiState.value as? QuizUiState.InProgress ?: return
        val next = state.session.next()
        if (!next.isFinished) {
            _uiState.value = state.copy(session = next)
            return
        }
        // Decide "new best" synchronously from what we loaded, so the results screen shows at once;
        // the repository re-checks atomically while saving.
        val result = next.result()
        val best = previousBest
        _uiState.value = QuizUiState.Finished(
            deckTitle = state.deckTitle,
            result = result,
            bestStreak = next.bestStreak,
            isNewBest = result.score > 0 && (best == null || result.score > best.score),
            previousBest = best,
        )
        viewModelScope.launch { suspendRunCatching { scoreRepository.recordResult(deckId, result) } }
    }
}
