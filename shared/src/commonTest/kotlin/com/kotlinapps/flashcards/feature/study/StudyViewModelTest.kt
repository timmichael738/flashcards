package com.kotlinapps.flashcards.feature.study

import com.kotlinapps.flashcards.testing.FakeDeckRepository
import com.kotlinapps.flashcards.testing.MainDispatcherTest
import com.kotlinapps.flashcards.testing.testDeck
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The Main dispatcher is unconfined, so loading completes synchronously inside the constructor. */
class StudyViewModelTest : MainDispatcherTest() {

    private val deck = testDeck(size = 5)

    private fun viewModel(repository: FakeDeckRepository = FakeDeckRepository(listOf(deck))) =
        StudyViewModel(deck.id, repository, Random(7))

    private val StudyViewModel.ready get() = assertIs<StudyUiState.Ready>(uiState.value)

    @Test
    fun loadsDeckInOriginalOrder() {
        val vm = viewModel()
        assertEquals(deck.cards, vm.ready.cards)
        assertEquals(0, vm.ready.index)
        assertFalse(vm.ready.isFlipped)
        assertEquals(deck.title, vm.ready.deckTitle)
    }

    @Test
    fun flip_togglesAndNavigationResetsIt() {
        val vm = viewModel()
        vm.onFlip()
        assertTrue(vm.ready.isFlipped)
        vm.onNext()
        assertEquals(1, vm.ready.index)
        assertFalse(vm.ready.isFlipped)
    }

    @Test
    fun navigation_staysWithinBounds() {
        val vm = viewModel()
        vm.onPrevious()
        assertEquals(0, vm.ready.index)
        repeat(10) { vm.onNext() }
        assertEquals(4, vm.ready.index)
        assertFalse(vm.ready.hasNext)
        assertTrue(vm.ready.hasPrevious)
    }

    @Test
    fun shuffle_reordersAndUnshuffleRestores() {
        val vm = viewModel()
        vm.onNext()
        vm.onToggleShuffle()
        assertTrue(vm.ready.isShuffled)
        assertEquals(0, vm.ready.index)
        assertEquals(deck.cards.toSet(), vm.ready.cards.toSet())
        assertNotEquals(deck.cards, vm.ready.cards)

        vm.onToggleShuffle()
        assertEquals(deck.cards, vm.ready.cards)
    }

    @Test
    fun unknownDeck_isNotFound() {
        val vm = StudyViewModel("missing", FakeDeckRepository(listOf(deck)), Random(0))
        assertEquals(StudyUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun failure_showsError_andRetryRecovers() {
        val repository = FakeDeckRepository(listOf(deck)).apply { failWith = IllegalStateException() }
        val vm = viewModel(repository)
        assertEquals(StudyUiState.Error, vm.uiState.value)

        repository.failWith = null
        vm.retry()
        assertIs<StudyUiState.Ready>(vm.uiState.value)
    }
}
