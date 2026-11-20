package com.kotlinapps.flashcards.core.domain

import com.kotlinapps.flashcards.core.model.Flashcard
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QuizGeneratorTest {

    private val cards = (1..8).map { Flashcard("Q$it", "A$it") }

    @Test
    fun everyQuestionHasItsAnswerAndDistinctOptions() {
        val questions = QuizGenerator.generate(cards, questionCount = 8, random = Random(1))
        assertEquals(8, questions.size)
        questions.forEach { q ->
            val expected = "A" + q.prompt.removePrefix("Q")
            assertEquals(expected, q.correctAnswer)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
        }
        // Each card is asked once.
        assertEquals(cards.map { it.front }.toSet(), questions.map { it.prompt }.toSet())
    }

    @Test
    fun questionCount_isCappedAtDeckSize() {
        assertEquals(8, QuizGenerator.generate(cards, questionCount = 20, random = Random(2)).size)
        assertEquals(3, QuizGenerator.generate(cards, questionCount = 3, random = Random(2)).size)
    }

    @Test
    fun smallDeck_getsFewerOptions() {
        val small = listOf(Flashcard("a", "1"), Flashcard("b", "2"))
        QuizGenerator.generate(small, 5, Random(3)).forEach { assertEquals(2, it.options.size) }
    }

    @Test
    fun duplicateAnswers_neverAppearAsDistractors() {
        val withDuplicates = listOf(Flashcard("a", "same"), Flashcard("b", "same"), Flashcard("c", "other"))
        QuizGenerator.generate(withDuplicates, 3, Random(4)).forEach { q ->
            assertEquals(q.options.distinct(), q.options)
        }
    }

    @Test
    fun sameSeed_givesSameQuiz_differentSeed_differs() {
        val a = QuizGenerator.generate(cards, 8, Random(42))
        assertEquals(a, QuizGenerator.generate(cards, 8, Random(42)))
        assertTrue((1..10).any { QuizGenerator.generate(cards, 8, Random(it)) != a })
    }

    @Test
    fun invalidArguments_areRejected() {
        assertFailsWith<IllegalArgumentException> { QuizGenerator.generate(cards, 0, Random(0)) }
        assertFailsWith<IllegalArgumentException> { QuizGenerator.generate(cards, 5, Random(0), optionCount = 1) }
    }
}
