package com.kotlinapps.flashcards.feature.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.flashcards.core.designsystem.component.ErrorState
import com.kotlinapps.flashcards.core.designsystem.component.LoadingState
import com.kotlinapps.flashcards.core.designsystem.theme.FlashcardsTheme
import com.kotlinapps.flashcards.core.domain.QuizQuestion
import com.kotlinapps.flashcards.core.domain.QuizSession
import com.kotlinapps.flashcards.core.model.BestScore
import com.kotlinapps.flashcards.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun QuizRoute(
    deckId: String,
    onNavigateUp: () -> Unit,
    viewModel: QuizViewModel = koinViewModel(key = "quiz-$deckId", parameters = { parametersOf(deckId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QuizScreen(
        uiState = uiState,
        onAnswer = viewModel::onAnswer,
        onNext = viewModel::onNext,
        onPlayAgain = viewModel::startNewQuiz,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuizScreen(
    uiState: QuizUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPlayAgain: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is QuizUiState.InProgress -> uiState.deckTitle
        is QuizUiState.Finished -> uiState.deckTitle
        else -> stringResource(Res.string.quiz)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.navigate_up))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (uiState) {
                QuizUiState.Loading -> LoadingState()
                QuizUiState.Error -> ErrorState(stringResource(Res.string.error_loading_decks), onPlayAgain)
                QuizUiState.NotFound -> ErrorState(stringResource(Res.string.error_deck_not_found), onRetry = null)
                is QuizUiState.InProgress -> QuestionContent(uiState.session, onAnswer, onNext)
                is QuizUiState.Finished -> ResultsContent(uiState, onPlayAgain, onNavigateUp)
            }
        }
    }
}

@Composable
private fun QuestionContent(session: QuizSession, onAnswer: (Int) -> Unit, onNext: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = (session.currentIndex + if (session.isAnswered) 1f else 0f) / session.total,
        label = "quizProgress",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        ScoreRow(session)
        Spacer(Modifier.height(16.dp))

        // Each new question slides in; answering within a question does not re-run the transition.
        AnimatedContent(
            targetState = session,
            contentKey = { it.currentIndex },
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            modifier = Modifier.widthIn(max = 560.dp),
            label = "question",
        ) { shown ->
            // Keyed by index: the outgoing question keeps its answered state while it slides away.
            if (!shown.isFinished) QuestionCard(shown.current, shown.selectedIndex, onAnswer)
        }

        Spacer(Modifier.height(16.dp))
        AnimatedVisibility(
            visible = session.isAnswered,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val correct = session.selectedIndex == session.current.correctIndex
                Text(
                    text = if (correct) {
                        stringResource(Res.string.correct)
                    } else {
                        stringResource(Res.string.incorrect, session.current.correctAnswer)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(
                        stringResource(
                            if (session.isLastQuestion) Res.string.see_results else Res.string.next_question,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(session: QuizSession) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.question_position, session.currentIndex + 1, session.total),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(visible = session.streak >= 2, enter = scaleIn() + fadeIn(), exit = fadeOut()) {
            Text(
                stringResource(Res.string.streak_label, session.streak),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        // The score rolls upwards whenever it changes.
        AnimatedContent(
            targetState = session.score,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "score",
        ) { score ->
            Text(
                stringResource(Res.string.score_label, score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun QuestionCard(question: QuizQuestion, selectedIndex: Int?, onAnswer: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(
                question.prompt,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
            )
        }
        question.options.forEachIndexed { index, option ->
            OptionButton(
                text = option,
                state = when {
                    selectedIndex == null -> OptionState.Idle
                    index == question.correctIndex -> OptionState.Correct
                    index == selectedIndex -> OptionState.Wrong
                    else -> OptionState.Dimmed
                },
                onClick = { onAnswer(index) },
            )
        }
    }
}

private enum class OptionState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun OptionButton(text: String, state: OptionState, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (state) {
            OptionState.Correct -> scheme.secondaryContainer
            OptionState.Wrong -> scheme.errorContainer
            else -> scheme.surface
        },
        label = "optionContainer",
    )
    val content by animateColorAsState(
        when (state) {
            OptionState.Correct -> scheme.onSecondaryContainer
            OptionState.Wrong -> scheme.onErrorContainer
            OptionState.Dimmed -> scheme.onSurface.copy(alpha = 0.5f)
            OptionState.Idle -> scheme.onSurface
        },
        label = "optionContent",
    )
    // A wrong pick gives a quick shake.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == OptionState.Wrong) {
            for (offset in listOf(12f, -10f, 8f, -6f, 0f)) shake.animateTo(offset, tween(50))
        }
    }
    val description = when (state) {
        OptionState.Correct -> stringResource(Res.string.option_correct_description, text)
        OptionState.Wrong -> stringResource(Res.string.option_wrong_description, text)
        else -> text
    }
    OutlinedCard(
        onClick = onClick,
        enabled = state == OptionState.Idle,
        colors = CardDefaults.outlinedCardColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content,
        ),
        border = BorderStroke(1.dp, if (state == OptionState.Idle) scheme.outline else Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer { translationX = shake.value * density }
            .semantics { contentDescription = description },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when (state) {
                OptionState.Correct -> Icon(painterResource(Res.drawable.ic_check), contentDescription = null)
                OptionState.Wrong -> Icon(painterResource(Res.drawable.ic_close), contentDescription = null)
                else -> Unit
            }
        }
    }
}

@Composable
private fun ResultsContent(state: QuizUiState.Finished, onPlayAgain: () -> Unit, onBackToDecks: () -> Unit) {
    // The trophy pops in and the score counts up from zero.
    val trophyScale = remember { Animatable(0f) }
    val shownScore = remember { Animatable(0f) }
    LaunchedEffect(state) {
        trophyScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    LaunchedEffect(state) {
        shownScore.animateTo(state.result.score.toFloat(), tween(durationMillis = 1_000))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_trophy),
            contentDescription = null,
            tint = if (state.isNewBest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = trophyScale.value
                    scaleY = trophyScale.value
                },
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.results_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.results_score, shownScore.value.toInt()),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            // Screen readers get the final score, not the count-up.
            modifier = Modifier.semantics {
                contentDescription = state.result.score.toString()
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(
                Res.string.results_correct,
                state.result.correct,
                state.result.total,
                "${state.accuracyPercent}%",
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(Res.string.results_best_streak, state.bestStreak),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                when {
                    state.accuracyPercent >= 80 -> Res.string.results_great
                    state.accuracyPercent >= 50 -> Res.string.results_good
                    else -> Res.string.results_keep_going
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        if (state.isNewBest) {
            Text(
                stringResource(Res.string.new_best),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
        } else if (state.previousBest != null) {
            Text(
                stringResource(Res.string.previous_best, state.previousBest.score),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBackToDecks) { Text(stringResource(Res.string.back_to_decks)) }
            Button(onClick = onPlayAgain) {
                Icon(painterResource(Res.drawable.ic_replay), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.play_again))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizInProgressPreview() {
    val question = QuizQuestion("Kenya", listOf("Nairobi", "Cairo", "Abuja", "Rabat"), correctIndex = 0)
    FlashcardsTheme(dynamicColor = false) {
        QuizScreen(
            uiState = QuizUiState.InProgress(
                "World Capitals",
                QuizSession(listOf(question, question), score = 210, correctCount = 2, streak = 2).answer(2),
            ),
            onAnswer = {},
            onNext = {},
            onPlayAgain = {},
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizResultsPreview() {
    FlashcardsTheme(dynamicColor = false) {
        QuizScreen(
            uiState = QuizUiState.Finished(
                deckTitle = "World Capitals",
                result = BestScore(score = 1_020, correct = 9, total = 10),
                bestStreak = 6,
                isNewBest = true,
                previousBest = BestScore(800, 7, 10),
            ),
            onAnswer = {},
            onNext = {},
            onPlayAgain = {},
            onNavigateUp = {},
        )
    }
}
