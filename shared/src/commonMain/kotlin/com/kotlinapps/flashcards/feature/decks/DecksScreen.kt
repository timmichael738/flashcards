package com.kotlinapps.flashcards.feature.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.flashcards.core.designsystem.component.ErrorState
import com.kotlinapps.flashcards.core.designsystem.component.LoadingState
import com.kotlinapps.flashcards.core.designsystem.component.colors
import com.kotlinapps.flashcards.core.designsystem.theme.FlashcardsTheme
import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.core.model.Deck
import com.kotlinapps.flashcards.core.model.DeckAccent
import com.kotlinapps.flashcards.core.model.Flashcard
import com.kotlinapps.flashcards.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DecksRoute(
    onStudy: (deckId: String) -> Unit,
    onQuiz: (deckId: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DecksViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DecksScreen(
        uiState = uiState,
        onStudy = onStudy,
        onQuiz = onQuiz,
        onRetry = viewModel::retry,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DecksScreen(
    uiState: DecksUiState,
    onStudy: (String) -> Unit,
    onQuiz: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.decks_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(Res.drawable.ic_settings), stringResource(Res.string.settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                DecksUiState.Loading -> LoadingState()
                DecksUiState.Error -> ErrorState(stringResource(Res.string.error_loading_decks), onRetry)
                is DecksUiState.Success ->
                    if (uiState.items.isEmpty()) {
                        ErrorState(stringResource(Res.string.decks_empty), onRetry = null)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.items, key = { it.deck.id }) { item ->
                                DeckCard(
                                    item = item,
                                    onStudy = { onStudy(item.deck.id) },
                                    onQuiz = { onQuiz(item.deck.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun DeckCard(
    item: DeckItem,
    onStudy: () -> Unit,
    onQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deck = item.deck
    val accent = deck.accent.colors()
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accent.container, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = deck.title.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = accent.content,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        deck.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        pluralStringResource(Res.plurals.card_count, deck.cards.size, deck.cards.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (deck.description.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    deck.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            val best = item.best
            Text(
                text = if (best == null) {
                    stringResource(Res.string.not_played)
                } else {
                    stringResource(Res.string.best_score, best.score, best.correct, best.total)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onStudy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.study))
                }
                Button(onClick = onQuiz, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.quiz))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DecksScreenPreview() {
    val cards = listOf(Flashcard("Japan", "Tokyo"), Flashcard("Kenya", "Nairobi"))
    FlashcardsTheme(dynamicColor = false) {
        DecksScreen(
            uiState = DecksUiState.Success(
                listOf(
                    DeckItem(Deck("a", "World Capitals", "Match countries with capitals.", DeckAccent.TEAL, cards), null),
                    DeckItem(
                        Deck("b", "Kotlin Basics", "Core language features.", DeckAccent.PURPLE, cards),
                        BestScore(score = 980, correct = 8, total = 10),
                    ),
                ),
            ),
            onStudy = {},
            onQuiz = {},
            onRetry = {},
            onOpenSettings = {},
        )
    }
}
