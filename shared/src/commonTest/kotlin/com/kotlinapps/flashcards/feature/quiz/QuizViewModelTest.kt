package com.kotlinapps.flashcards.feature.quiz

import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.core.model.UserPreferences
import com.kotlinapps.flashcards.testing.FakeDeckRepository
import com.kotlinapps.flashcards.testing.FakeScoreRepository
import com.kotlinapps.flashcards.testing.FakeUserPreferencesRepository
import com.kotlinapps.flashcards.testing.MainDispatcherTest
import com.kotlinapps.flashcards.testing.testDeck
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The Main dispatcher is unconfined, so every action completes synchronously. */
class QuizViewModelTest : MainDispatcherTest() {

    private val deck = testDeck(size = 8)
    private val scores = FakeScoreRepository()

    private fun viewModel(
        questions: Int = 5,
        repository: FakeDeckRepository = FakeDeckRepository(listOf(deck)),
        deckId: String = deck.id,
    ) = QuizViewModel(
        deckId = deckId,
        deckRepository = repository,
        scoreRepository = scores,
        preferencesRepository = FakeUserPreferencesRepository(UserPreferences(questionsPerQuiz = questions)),
        random = Random(11),
    )

    private val QuizViewModel.inProgress get() = assertIs<QuizUiState.InProgress>(uiState.value)

    /** Answers every remaining question, correctly or not, and advances. */
    private fun QuizViewModel.playToEnd(correct: (Int) -> Boolean) {
        var i = 0
        while (uiState.value is QuizUiState.InProgress) {
            val q = inProgress.session.current
            onAnswer(if (correct(i)) q.correctIndex else (q.correctIndex + 1) % q.options.size)
            onNext()
            i++
        }
    }

    @Test
    fun startsQuiz_withConfiguredLength() {
        val vm = viewModel(questions = 5)
        assertEquals(5, vm.inProgress.session.total)
        assertEquals(deck.title, vm.inProgress.deckTitle)
    }

    @Test
    fun answering_updatesSession_andNextAdvances() {
        val vm = viewModel()
        val correct = vm.inProgress.session.current.correctIndex
        vm.onAnswer(correct)
        assertEquals(100, vm.inProgress.session.score)
        assertTrue(vm.inProgress.session.isAnswered)

        vm.onNext()
        assertEquals(1, vm.inProgress.session.currentIndex)
        assertFalse(vm.inProgress.session.isAnswered)
    }

    @Test
    fun perfectRun_finishesAsNewBest_andIsSaved() {
        val vm = viewModel(questions = 5)
        vm.playToEnd { true }

        val finished = assertIs<QuizUiState.Finished>(vm.uiState.value)
        val expected = BestScore(score = 100 + 110 + 120 + 130 + 140, correct = 5, total = 5)
        assertEquals(expected, finished.result)
        assertEquals(100, finished.accuracyPercent)
        assertEquals(5, finished.bestStreak)
        assertTrue(finished.isNewBest)
        assertEquals(null, finished.previousBest)
        assertEquals(listOf(deck.id to expected), scores.recorded)
    }

    @Test
    fun worseRun_isNotNewBest_andShowsPreviousBest() {
        val vm = viewModel(questions = 5)
        vm.playToEnd { true }
        vm.startNewQuiz()
        vm.playToEnd { it == 0 }

        val finished = assertIs<QuizUiState.Finished>(vm.uiState.value)
        assertEquals(1, finished.result.correct)
        assertFalse(finished.isNewBest)
        assertEquals(600, finished.previousBest?.score)
    }

    @Test
    fun zeroScore_isNeverCelebrated() {
        val vm = viewModel(questions = 5)
        vm.playToEnd { false }
        assertFalse(assertIs<QuizUiState.Finished>(vm.uiState.value).isNewBest)
    }

    @Test
    fun playAgain_startsFreshSession_withoutReloadingDeck() {
        val repository = FakeDeckRepository(listOf(deck))
        val vm = viewModel(repository = repository)
        vm.playToEnd { true }
        vm.startNewQuiz()

        assertEquals(0, vm.inProgress.session.currentIndex)
        assertEquals(0, vm.inProgress.session.score)
        assertEquals(1, repository.calls)
    }

    @Test
    fun unknownDeck_isNotFound_andFailureIsError() {
        assertEquals(QuizUiState.NotFound, viewModel(deckId = "missing").uiState.value)

        val failing = FakeDeckRepository().apply { failWith = IllegalStateException() }
        assertEquals(QuizUiState.Error, viewModel(repository = failing).uiState.value)
    }
}
