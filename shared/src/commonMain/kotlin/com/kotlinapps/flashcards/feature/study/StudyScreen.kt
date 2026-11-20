package com.kotlinapps.flashcards.feature.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.flashcards.core.designsystem.component.ErrorState
import com.kotlinapps.flashcards.core.designsystem.component.FlipCard
import com.kotlinapps.flashcards.core.designsystem.component.LoadingState
import com.kotlinapps.flashcards.core.designsystem.theme.FlashcardsTheme
import com.kotlinapps.flashcards.core.model.Flashcard
import com.kotlinapps.flashcards.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun StudyRoute(
    deckId: String,
    onNavigateUp: () -> Unit,
    viewModel: StudyViewModel = koinViewModel(key = "study-$deckId", parameters = { parametersOf(deckId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudyScreen(
        uiState = uiState,
        onFlip = viewModel::onFlip,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onToggleShuffle = viewModel::onToggleShuffle,
        onRetry = viewModel::retry,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudyScreen(
    uiState: StudyUiState,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onRetry: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text((uiState as? StudyUiState.Ready)?.deckTitle ?: stringResource(Res.string.study)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.navigate_up))
                    }
                },
                actions = {
                    if (uiState is StudyUiState.Ready) {
                        IconToggleButton(checked = uiState.isShuffled, onCheckedChange = { onToggleShuffle() }) {
                            Icon(
                                painterResource(Res.drawable.ic_shuffle),
                                stringResource(if (uiState.isShuffled) Res.string.unshuffle else Res.string.shuffle),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                StudyUiState.Loading -> LoadingState()
                StudyUiState.Error -> ErrorState(stringResource(Res.string.error_loading_decks), onRetry)
                StudyUiState.NotFound -> ErrorState(stringResource(Res.string.error_deck_not_found), onRetry = null)
                is StudyUiState.Ready -> StudyContent(uiState, onFlip, onNext, onPrevious)
            }
        }
    }
}

@Composable
private fun StudyContent(
    state: StudyUiState.Ready,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            progress = { (state.index + 1f) / state.cards.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        // Slide in from the direction of travel; a shuffle (back to index 0) also slides "backwards".
        AnimatedContent(
            targetState = state.index to state.cards,
            transitionSpec = {
                val forward = targetState.first > initialState.first
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            },
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 520.dp)
                .pointerInput(state.index) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            when {
                                total < -SWIPE_THRESHOLD_PX -> onNext()
                                total > SWIPE_THRESHOLD_PX -> onPrevious()
                            }
                        },
                    ) { _, dragAmount -> total += dragAmount }
                },
            label = "card",
        ) { (index, cards) ->
            val card = cards[index]
            // Only the visible card follows the flip state; outgoing cards keep their face.
            val flipped = index == state.index && cards == state.cards && state.isFlipped
            CardFaces(card = card, isFlipped = flipped, onFlip = onFlip)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.tap_to_flip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FilledTonalIconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                Icon(painterResource(Res.drawable.ic_navigate_before), stringResource(Res.string.previous_card))
            }
            Text(
                stringResource(Res.string.card_position, state.index + 1, state.cards.size),
                style = MaterialTheme.typography.titleMedium,
            )
            FilledTonalIconButton(onClick = onNext, enabled = state.hasNext) {
                Icon(painterResource(Res.drawable.ic_navigate_next), stringResource(Res.string.next_card))
            }
        }
    }
}

@Composable
private fun CardFaces(card: Flashcard, isFlipped: Boolean, onFlip: () -> Unit) {
    val description = if (isFlipped) {
        stringResource(Res.string.card_back_description, card.back)
    } else {
        stringResource(Res.string.card_front_description, card.front)
    }
    FlipCard(
        isFlipped = isFlipped,
        onClick = onFlip,
        frontColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        backColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        front = { CardFace(label = stringResource(Res.string.question_label), text = card.front) },
        back = { CardFace(label = stringResource(Res.string.answer_label), text = card.back) },
    )
}

@Composable
private fun CardFace(label: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    }
}

private const val SWIPE_THRESHOLD_PX = 120f

@Preview(showBackground = true)
@Composable
private fun StudyScreenPreview() {
    FlashcardsTheme(dynamicColor = false) {
        StudyScreen(
            uiState = StudyUiState.Ready(
                deckTitle = "World Capitals",
                cards = listOf(Flashcard("Japan", "Tokyo"), Flashcard("Kenya", "Nairobi")),
            ),
            onFlip = {},
            onNext = {},
            onPrevious = {},
            onToggleShuffle = {},
            onRetry = {},
            onNavigateUp = {},
        )
    }
}
