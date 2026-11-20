package com.kotlinapps.flashcards.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A card that turns over around its vertical axis. The front is drawn for the first half of the
 * rotation and the back (counter-rotated so its text is not mirrored) for the second half.
 */
@Composable
fun FlipCard(
    isFlipped: Boolean,
    onClick: () -> Unit,
    frontColor: Color,
    backColor: Color,
    modifier: Modifier = Modifier,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "cardRotation",
    )
    val showingBack = rotation > 90f
    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            // A larger camera distance keeps the perspective subtle instead of stretching the card.
            cameraDistance = 12f * density
        },
        colors = CardDefaults.cardColors(containerColor = if (showingBack) backColor else frontColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { if (showingBack) rotationY = 180f },
            contentAlignment = Alignment.Center,
            content = if (showingBack) back else front,
        )
    }
}
