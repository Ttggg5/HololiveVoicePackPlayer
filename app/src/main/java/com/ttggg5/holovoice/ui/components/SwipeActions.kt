package com.ttggg5.holovoice.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer

/** How far (fraction of the item width) a swipe must travel before its action fires. */
private const val TRIGGER_FRACTION = 0.5f

/**
 * Swipe right → play next, swipe left → toggle favorite. The item always springs back into place.
 */
@Composable
fun ClipSwipeActions(
    isFavorite: Boolean,
    onPlayNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Settings › Gestures: clip swipes can be turned off.
    val enabled by LocalContext.current.appContainer.uiPrefs.clipSwipes.state.collectAsStateWithLifecycle()
    if (!enabled) {
        androidx.compose.foundation.layout.Box(modifier.clip(shape)) { content() }
        return
    }
    val haptics = LocalHapticFeedback.current
    val playNext by rememberUpdatedState(onPlayNext)
    val toggle by rememberUpdatedState(onToggleFavorite)
    // Width of the item, and a way for confirmValueChange to read the state it belongs to.
    var widthPx by remember { mutableFloatStateOf(0f) }
    val stateHolder = remember { arrayOfNulls<SwipeToDismissBoxState>(1) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Only a deliberate swipe across half the item counts; quick short flicks spring back.
            val offset = stateHolder[0]?.let { runCatching { it.requireOffset() }.getOrNull() } ?: 0f
            val farEnough = widthPx > 0f && abs(offset) >= widthPx * TRIGGER_FRACTION
            if (farEnough) {
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        playNext()
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        toggle()
                    }
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
            false // never stay swiped away
        },
        positionalThreshold = { it * 0.55f },
    )
    stateHolder[0] = state
    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(shape).onSizeChanged { widthPx = it.width.toFloat() },
        backgroundContent = {
            val direction = state.dismissDirection
            val offset = runCatching { abs(state.requireOffset()) }.getOrDefault(0f)
            val progress = if (widthPx > 0f) (offset / (widthPx * TRIGGER_FRACTION)).coerceIn(0f, 1f) else 0f
            val ready = progress >= 1f
            val color by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipeBackground",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (direction == SwipeToDismissBoxValue.EndToStart) Arrangement.End else Arrangement.Start,
                // Fades in with the swipe distance, so a small accidental move barely shows.
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = progress }.background(color).padding(horizontal = 20.dp),
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(if (ready) " Release to play next" else " Play next", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Text(
                            (if (isFavorite) "unfavorite " else "favorite ").let { if (ready) "Release to $it" else it.replaceFirstChar(Char::uppercase) },
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Icon(
                            if (isFavorite) Icons.Outlined.StarBorder else Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        },
    ) { content() }
}
