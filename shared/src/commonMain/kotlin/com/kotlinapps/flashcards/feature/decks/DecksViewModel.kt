package com.kotlinapps.flashcards.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.flashcards.core.data.deck.DeckRepository
import com.kotlinapps.flashcards.core.data.repository.ScoreRepository
import com.kotlinapps.flashcards.core.domain.suspendRunCatching
import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.core.model.Deck
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class DeckItem(val deck: Deck, val best: BestScore?)

sealed interface DecksUiState {
    data object Loading : DecksUiState
    data object Error : DecksUiState
    data class Success(val items: List<DeckItem>) : DecksUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DecksViewModel(
    private val deckRepository: DeckRepository,
    scoreRepository: ScoreRepository,
) : ViewModel() {

    private val loadRequests = MutableStateFlow(0)

    /** Null while loading. Re-runs on every [retry]. */
    private val decks = loadRequests.flatMapLatest {
        flow {
            emit(null)
            emit(suspendRunCatching { deckRepository.getDecks() })
        }
    }

    val uiState: StateFlow<DecksUiState> = combine(decks, scoreRepository.bestScores) { result, scores ->
        when {
            result == null -> DecksUiState.Loading
            result.isFailure -> DecksUiState.Error
            else -> DecksUiState.Success(result.getOrThrow().map { DeckItem(it, scores[it.id]) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState.Loading)

    fun retry() = loadRequests.update { it + 1 }
}
