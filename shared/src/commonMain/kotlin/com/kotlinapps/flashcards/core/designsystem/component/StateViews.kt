package com.kotlinapps.flashcards.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotlinapps.flashcards.core.model.DeckAccent
import com.kotlinapps.flashcards.resources.Res
import com.kotlinapps.flashcards.resources.ic_error
import com.kotlinapps.flashcards.resources.retry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Full-screen message with an optional retry button. */
@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(Res.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
            }
        }
    }
}

/** Container/content color pair for a deck accent, derived from the theme so it works in light and dark. */
data class AccentColors(val container: Color, val content: Color)

@Composable
fun DeckAccent.colors(): AccentColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        DeckAccent.PURPLE -> AccentColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        DeckAccent.TEAL -> AccentColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
        DeckAccent.ORANGE -> AccentColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        DeckAccent.BLUE -> AccentColors(scheme.surfaceContainerHighest, scheme.onSurface)
    }
}
