package com.kotlinapps.flashcards.feature.decks

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.testing.FakeDeckRepository
import com.kotlinapps.flashcards.testing.FakeScoreRepository
import com.kotlinapps.flashcards.testing.MainDispatcherTest
import com.kotlinapps.flashcards.testing.testDeck
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DecksViewModelTest : MainDispatcherTest() {

    private suspend fun ReceiveTurbine<DecksUiState>.awaitLoaded(): DecksUiState {
        var item = awaitItem()
        while (item is DecksUiState.Loading) item = awaitItem()
        return item
    }

    @Test
    fun decks_areCombinedWithBestScores() = runTest {
        val best = BestScore(500, 4, 5)
        val repository = FakeDeckRepository(listOf(testDeck("a"), testDeck("b")))
        val viewModel = DecksViewModel(repository, FakeScoreRepository(mapOf("b" to best)))

        viewModel.uiState.test {
            val state = assertIs<DecksUiState.Success>(awaitLoaded())
            assertEquals(listOf("a", "b"), state.items.map { it.deck.id })
            assertEquals(listOf(null, best), state.items.map { it.best })
        }
    }

    @Test
    fun scoreChanges_updateTheList() = runTest {
        val scores = FakeScoreRepository()
        val viewModel = DecksViewModel(FakeDeckRepository(), scores)

        viewModel.uiState.test {
            assertIs<DecksUiState.Success>(awaitLoaded())
            scores.recordResult("capitals", BestScore(300, 3, 3))
            val updated = assertIs<DecksUiState.Success>(awaitItem())
            assertEquals(300, updated.items.single().best?.score)
        }
    }

    @Test
    fun failure_showsError_andRetryRecovers() = runTest {
        val repository = FakeDeckRepository().apply { failWith = IllegalStateException("broken") }
        val viewModel = DecksViewModel(repository, FakeScoreRepository())

        viewModel.uiState.test {
            assertEquals(DecksUiState.Error, awaitLoaded())
            repository.failWith = null
            viewModel.retry()
            assertIs<DecksUiState.Success>(awaitLoaded())
        }
    }
}
