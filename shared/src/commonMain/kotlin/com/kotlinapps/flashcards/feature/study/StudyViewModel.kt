package com.kotlinapps.flashcards.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.flashcards.core.data.deck.DeckRepository
import com.kotlinapps.flashcards.core.domain.suspendRunCatching
import com.kotlinapps.flashcards.core.model.Flashcard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface StudyUiState {
    data object Loading : StudyUiState
    data object NotFound : StudyUiState
    data object Error : StudyUiState

    data class Ready(
        val deckTitle: String,
        val cards: List<Flashcard>,
        val index: Int = 0,
        val isFlipped: Boolean = false,
        val isShuffled: Boolean = false,
    ) : StudyUiState {
        val card: Flashcard get() = cards[index]
        val hasPrevious: Boolean get() = index > 0
        val hasNext: Boolean get() = index < cards.lastIndex
    }
}

class StudyViewModel(
    private val deckId: String,
    private val deckRepository: DeckRepository,
    private val random: Random,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    /** Cards in deck order, used to undo a shuffle. */
    private var originalCards: List<Flashcard> = emptyList()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = StudyUiState.Loading
        viewModelScope.launch {
            _uiState.value = suspendRunCatching { deckRepository.getDeck(deckId) }.fold(
                onSuccess = { deck ->
                    if (deck == null) {
                        StudyUiState.NotFound
                    } else {
                        originalCards = deck.cards
                        StudyUiState.Ready(deckTitle = deck.title, cards = deck.cards)
                    }
                },
                onFailure = { StudyUiState.Error },
            )
        }
    }

    private inline fun updateReady(transform: (StudyUiState.Ready) -> StudyUiState.Ready) =
        _uiState.update { if (it is StudyUiState.Ready) transform(it) else it }

    fun onFlip() = updateReady { it.copy(isFlipped = !it.isFlipped) }

    /** Every move shows the new card question-side up. */
    fun onNext() = updateReady { if (it.hasNext) it.copy(index = it.index + 1, isFlipped = false) else it }

    fun onPrevious() = updateReady { if (it.hasPrevious) it.copy(index = it.index - 1, isFlipped = false) else it }

    fun onToggleShuffle() = updateReady {
        val shuffle = !it.isShuffled
        it.copy(
            cards = if (shuffle) originalCards.shuffled(random) else originalCards,
            index = 0,
            isFlipped = false,
            isShuffled = shuffle,
        )
    }
}
