package com.kotlinapps.flashcards.core.data.deck

import com.kotlinapps.flashcards.core.model.Deck
import com.kotlinapps.flashcards.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Supplies the raw deck file. Bundled in the app today; could be a network source later. */
fun interface DeckSource {
    suspend fun read(): String
}

internal class BundledDeckSource : DeckSource {
    override suspend fun read(): String = Res.readBytes(DECKS_FILE).decodeToString()

    private companion object {
        const val DECKS_FILE = "files/decks.json"
    }
}

interface DeckRepository {
    /** All decks. Throws if the deck file is unreadable or invalid. */
    suspend fun getDecks(): List<Deck>

    suspend fun getDeck(id: String): Deck?
}

/** Reads and parses the deck file once, then serves it from memory. Failures are not cached, so retry works. */
class DefaultDeckRepository(private val source: DeckSource) : DeckRepository {

    private val mutex = Mutex()
    private var cache: List<Deck>? = null

    override suspend fun getDecks(): List<Deck> = mutex.withLock {
        cache ?: DeckParser.parse(source.read()).also { cache = it }
    }

    override suspend fun getDeck(id: String): Deck? = getDecks().firstOrNull { it.id == id }
}
