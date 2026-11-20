package com.kotlinapps.flashcards.core.model

data class Deck(
    val id: String,
    val title: String,
    val description: String,
    val accent: DeckAccent,
    val cards: List<Flashcard>,
)

data class Flashcard(
    val front: String,
    val back: String,
)

/** Theme-aware accent used for a deck's tile; mapped to color-scheme roles in the UI. */
enum class DeckAccent { PURPLE, TEAL, ORANGE, BLUE }

/** The best quiz result recorded for a deck. */
data class BestScore(
    val score: Int,
    val correct: Int,
    val total: Int,
)
