package com.kotlinapps.flashcards.core.data.deck

import com.kotlinapps.flashcards.core.model.DeckAccent
import com.kotlinapps.flashcards.core.model.Flashcard
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DeckParserTest {

    private fun file(decks: String, version: Int = 1) = """{ "version": $version, "decks": [ $decks ] }"""

    private val validDeck = """
        { "id": "d1", "title": " Capitals ", "description": "Cities", "color": "Teal", "extra": true,
          "cards": [ { "front": "Japan", "back": "Tokyo" }, { "front": " Kenya ", "back": "Nairobi" } ] }
    """

    @Test
    fun validFile_isParsedAndTrimmed() {
        val deck = DeckParser.parse(file(validDeck)).single()
        assertEquals("d1", deck.id)
        assertEquals("Capitals", deck.title)
        assertEquals(DeckAccent.TEAL, deck.accent)
        assertEquals(listOf(Flashcard("Japan", "Tokyo"), Flashcard("Kenya", "Nairobi")), deck.cards)
    }

    @Test
    fun unknownColor_fallsBackToPurple() {
        val deck = DeckParser.parse(file(validDeck.replace("Teal", "magenta"))).single()
        assertEquals(DeckAccent.PURPLE, deck.accent)
    }

    @Test
    fun blankCards_areDropped() {
        val json = file(
            """{ "id": "d", "title": "T", "cards": [
                { "front": "a", "back": "1" }, { "front": "  ", "back": "x" }, { "front": "b", "back": "2" } ] }""",
        )
        assertEquals(2, DeckParser.parse(json).single().cards.size)
    }

    @Test
    fun malformedJson_throwsParseException() {
        assertFailsWith<DeckParseException> { DeckParser.parse("{ not json") }
        assertFailsWith<DeckParseException> { DeckParser.parse("""{ "decks": [] }""") } // missing version
    }

    @Test
    fun newerVersion_isRejected() {
        assertFailsWith<DeckParseException> { DeckParser.parse(file(validDeck, version = 2)) }
    }

    @Test
    fun duplicateIds_areRejected() {
        assertFailsWith<DeckParseException> { DeckParser.parse(file("$validDeck, $validDeck")) }
    }

    @Test
    fun deckWithoutTwoDistinctAnswers_isRejected() {
        val json = file(
            """{ "id": "d", "title": "T", "cards": [ { "front": "a", "back": "same" }, { "front": "b", "back": "same" } ] }""",
        )
        assertFailsWith<DeckParseException> { DeckParser.parse(json) }
    }

    @Test
    fun repository_cachesSuccess_butNotFailure() = runTest {
        var reads = 0
        var fail = true
        val repository = DefaultDeckRepository {
            reads++
            if (fail) "broken" else file(validDeck)
        }

        assertFailsWith<DeckParseException> { repository.getDecks() }
        fail = false
        val first = repository.getDecks()
        assertSame(first, repository.getDecks())
        assertEquals("d1", repository.getDeck("d1")?.id)
        assertEquals(null, repository.getDeck("missing"))
        assertEquals(2, reads)
    }
}
