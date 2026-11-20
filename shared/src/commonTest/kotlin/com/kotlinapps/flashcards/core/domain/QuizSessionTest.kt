package com.kotlinapps.flashcards.core.domain

import com.kotlinapps.flashcards.core.model.BestScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class QuizSessionTest {

    /** Question i has its correct answer at option 0. */
    private fun session(count: Int) = QuizSession(
        (1..count).map { QuizQuestion("Q$it", listOf("right", "wrong1", "wrong2"), correctIndex = 0) },
    )

    private fun QuizSession.play(vararg picks: Int): QuizSession =
        picks.fold(this) { s, pick -> s.answer(pick).next() }

    @Test
    fun scoring_rewardsStreaks_withCappedBonus() {
        assertEquals(100, QuizScoring.pointsFor(1))
        assertEquals(110, QuizScoring.pointsFor(2))
        assertEquals(150, QuizScoring.pointsFor(6))
        assertEquals(150, QuizScoring.pointsFor(20))
        assertEquals(100 + 110 + 120, QuizScoring.maxScore(3))
        assertFailsWith<IllegalArgumentException> { QuizScoring.pointsFor(0) }
    }

    @Test
    fun correctAnswers_buildStreakAndScore() {
        val s = session(3).answer(0)
        assertTrue(s.isAnswered)
        assertEquals(100, s.score)
        assertEquals(1, s.streak)

        val done = session(3).play(0, 0, 0)
        assertTrue(done.isFinished)
        assertEquals(QuizScoring.maxScore(3), done.score)
        assertEquals(BestScore(score = 330, correct = 3, total = 3), done.result())
        assertEquals(3, done.bestStreak)
    }

    @Test
    fun wrongAnswer_resetsStreak_butKeepsBest() {
        val done = session(5).play(0, 0, 1, 0, 0)
        assertEquals(4, done.correctCount)
        assertEquals(2, done.bestStreak)
        assertEquals(100 + 110 + 0 + 100 + 110, done.score)
    }

    @Test
    fun answeringTwice_isIgnored() {
        val answered = session(2).answer(1)
        assertSame(answered, answered.answer(0))
        assertEquals(0, answered.score)
    }

    @Test
    fun next_beforeAnswering_isIgnored() {
        val s = session(2)
        assertSame(s, s.next())
    }

    @Test
    fun lastQuestion_flags() {
        val s = session(2).answer(0).next()
        assertTrue(s.isLastQuestion)
        assertFalse(s.isFinished)
        assertTrue(s.answer(0).next().isFinished)
    }

    @Test
    fun invalidOption_andEmptyQuiz_areRejected() {
        assertFailsWith<IllegalArgumentException> { session(1).answer(7) }
        assertFailsWith<IllegalArgumentException> { QuizSession(emptyList()) }
    }
}
