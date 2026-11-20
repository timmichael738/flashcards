package com.kotlinapps.flashcards.core.data.deck

import com.kotlinapps.flashcards.core.model.Deck
import com.kotlinapps.flashcards.core.model.DeckAccent
import com.kotlinapps.flashcards.core.model.Flashcard
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class DeckParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Parses and validates the bundled `decks.json`. Pure, so it is unit-tested on every platform. */
object DeckParser {

    private val json = Json {
        ignoreUnknownKeys = true // newer files may add fields without breaking older app versions
    }

    @Serializable
    private data class DeckFileDto(val version: Int, val decks: List<DeckDto>)

    @Serializable
    private data class DeckDto(
        val id: String,
        val title: String,
        val description: String = "",
        val color: String = "purple",
        val cards: List<CardDto>,
    )

    @Serializable
    private data class CardDto(val front: String, val back: String)

    const val SUPPORTED_VERSION = 1

    /** A quiz needs at least one wrong option, so every deck needs two distinct answers. */
    const val MIN_CARDS = 2

    fun parse(text: String): List<Deck> {
        val file = try {
            json.decodeFromString<DeckFileDto>(text)
        } catch (e: SerializationException) {
            throw DeckParseException("Malformed deck file", e)
        } catch (e: IllegalArgumentException) {
            throw DeckParseException("Malformed deck file", e)
        }
        if (file.version > SUPPORTED_VERSION) {
            throw DeckParseException("Unsupported deck file version ${file.version}")
        }

        val duplicateId = file.decks.groupBy { it.id }.entries.firstOrNull { it.value.size > 1 }?.key
        if (duplicateId != null) throw DeckParseException("Duplicate deck id '$duplicateId'")

        return file.decks.map { dto ->
            if (dto.id.isBlank() || dto.title.isBlank()) throw DeckParseException("Deck is missing an id or title")
            val cards = dto.cards
                .map { Flashcard(front = it.front.trim(), back = it.back.trim()) }
                .filter { it.front.isNotEmpty() && it.back.isNotEmpty() }
            if (cards.map { it.back }.distinct().size < MIN_CARDS) {
                throw DeckParseException("Deck '${dto.id}' needs at least $MIN_CARDS cards with different answers")
            }
            Deck(
                id = dto.id,
                title = dto.title.trim(),
                description = dto.description.trim(),
                accent = DeckAccent.entries.firstOrNull { it.name.equals(dto.color, ignoreCase = true) }
                    ?: DeckAccent.PURPLE,
                cards = cards,
            )
        }
    }
}
