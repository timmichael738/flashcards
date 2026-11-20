package com.kotlinapps.flashcards.core.domain

import com.kotlinapps.flashcards.core.model.BestScore

/**
 * Immutable state of one quiz run. Every action returns a new session, which keeps the
 * ViewModel trivial and makes the rules easy to unit-test.
 */
data class QuizSession(
    val questions: List<QuizQuestion>,
    val currentIndex: Int = 0,
    /** Option picked for the current question, or null while unanswered. */
    val selectedIndex: Int? = null,
    val score: Int = 0,
    val correctCount: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    init {
        require(questions.isNotEmpty()) { "A quiz needs at least one question" }
    }

    val isFinished: Boolean get() = currentIndex >= questions.size
    val current: QuizQuestion get() = questions[currentIndex]
    val isAnswered: Boolean get() = selectedIndex != null
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val total: Int get() = questions.size

    /** Locks in [optionIndex] for the current question. Ignored if already answered. */
    fun answer(optionIndex: Int): QuizSession {
        if (isFinished || isAnswered) return this
        require(optionIndex in current.options.indices) { "No option $optionIndex" }
        val correct = optionIndex == current.correctIndex
        val newStreak = if (correct) streak + 1 else 0
        return copy(
            selectedIndex = optionIndex,
            score = score + if (correct) QuizScoring.pointsFor(newStreak) else 0,
            correctCount = correctCount + if (correct) 1 else 0,
            streak = newStreak,
            bestStreak = maxOf(bestStreak, newStreak),
        )
    }

    /** Moves past an answered question. */
    fun next(): QuizSession {
        if (isFinished || !isAnswered) return this
        return copy(currentIndex = currentIndex + 1, selectedIndex = null)
    }

    fun result(): BestScore = BestScore(score = score, correct = correctCount, total = total)
}

object QuizScoring {
    const val BASE_POINTS = 100
    const val STREAK_BONUS_STEP = 10
    const val MAX_STREAK_BONUS = 50

    /** Points for a correct answer that makes the streak [streak] long (1 = first in a row). */
    fun pointsFor(streak: Int): Int {
        require(streak >= 1) { "streak must be at least 1" }
        return BASE_POINTS + ((streak - 1) * STREAK_BONUS_STEP).coerceAtMost(MAX_STREAK_BONUS)
    }

    /** Highest score possible for [questionCount] questions (all correct). */
    fun maxScore(questionCount: Int): Int = (1..questionCount).sumOf { pointsFor(it) }
}
