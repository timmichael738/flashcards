package com.kotlinapps.flashcards.core.domain

import com.kotlinapps.flashcards.core.model.Flashcard
import kotlin.random.Random

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
) {
    val correctAnswer: String get() = options[correctIndex]
}

/** Builds multiple-choice questions from a deck. Takes a [Random] so tests are deterministic. */
object QuizGenerator {

    const val DEFAULT_OPTION_COUNT = 4

    /**
     * Picks up to [questionCount] random cards. Each question offers the card's answer plus up to
     * `optionCount - 1` distinct wrong answers taken from other cards, in random order.
     */
    fun generate(
        cards: List<Flashcard>,
        questionCount: Int,
        random: Random,
        optionCount: Int = DEFAULT_OPTION_COUNT,
    ): List<QuizQuestion> {
        require(questionCount > 0) { "questionCount must be positive" }
        require(optionCount >= 2) { "optionCount must be at least 2" }
        val allAnswers = cards.map { it.back }.distinct()

        return cards.shuffled(random).take(questionCount).map { card ->
            val distractors = allAnswers
                .filter { it != card.back }
                .shuffled(random)
                .take(optionCount - 1)
            val options = (distractors + card.back).shuffled(random)
            QuizQuestion(prompt = card.front, options = options, correctIndex = options.indexOf(card.back))
        }
    }
}
