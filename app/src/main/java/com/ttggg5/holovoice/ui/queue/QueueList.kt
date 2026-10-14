package com.ttggg5.holovoice.ui.queue

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.PlayPauseIcon
import com.ttggg5.holovoice.ui.components.TrackList
import com.ttggg5.holovoice.ui.components.TrackRow

/**
 * The play queue: a now-playing card, shuffle / repeat / clear, then the clips (already played ones
 * faded). Tap to jump, drag to reorder, swipe to remove (with Undo). Used by the Queue screen, the
 * swipe-up panel in the player, and the player's side panel on large screens.
 */
@Composable
fun QueueList(player: PlayerController, modifier: Modifier = Modifier, showNowPlaying: Boolean = false) {
    val state by player.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val undo = LocalUndo.current
    var confirmClear by remember { mutableStateOf(false) }

    // Start with the current item in view (the header is list item 0).
    LaunchedEffect(Unit) {
        val current = state.queue.indexOfFirst { it.index == state.currentIndex }
        if (current > 1) listState.scrollToItem(current)
    }

    if (state.queue.isEmpty()) {
        EmptyMessage(
            "The queue is empty",
            "Play a topic, talent, playlist or clip to fill it.",
            modifier,
            icon = Icons.Filled.ClearAll,
        )
        return
    }

    val currentPos = state.queue.indexOfFirst { it.index == state.currentIndex }
    val positions = remember(state.queue) { state.queue.withIndex().associate { (i, item) -> item.key to i } }
    TrackList(
        items = state.queue,
        key = { it.key },
        row = { item ->
            val current = item.index == state.currentIndex
            TrackRow(
                item.title,
                item.subtitle,
                // Who is speaking, with the topic thumbnail on the corner.
                item.artworkUri ?: item.coverUri,
                highlighted = current,
                playing = current && state.isPlaying,
                badge = item.coverUri.takeIf { item.artworkUri != null && it != item.artworkUri.toString() },
                dimmed = (positions[item.key] ?: 0) < currentPos,
            )
        },
        onClick = { item, _ -> player.skipTo(item.index) },
        onRemove = { item ->
            player.removeQueueItem(item.index) { removed ->
                undo?.show("Removed \"${item.title}\" from the queue") { player.restoreQueueItem(item.index, removed) }
            }
        },
        // Without shuffle the play order is the item order, so list positions map to player indices.
        onMove = if (state.shuffle) null else { from, to -> player.moveQueueItem(from, to) },
        listState = listState,
        modifier = modifier,
        rowColor = Color.Transparent,
        header = {
            QueueHeader(
                state = state,
                player = player,
                showNowPlaying = showNowPlaying,
                left = (state.queue.size - currentPos - 1).coerceAtLeast(0),
                onClear = { confirmClear = true },
            )
        },
    )

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the queue?") },
            text = { Text("Playback stops and all ${state.queue.size} clips are removed from the queue.") },
            confirmButton = {
                TextButton(onClick = {
                    player.clearQueue()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun QueueHeader(state: PlayerState, player: PlayerController, showNowPlaying: Boolean, left: Int, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        // Hidden inside Now Playing, where the player itself is right next to the queue.
        if (showNowPlaying && state.clipId != null) NowPlayingCard(state, player)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            FilterChip(
                selected = state.shuffle,
                onClick = player::toggleShuffle,
                label = { Text("Shuffle") },
                leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = state.repeatMode != Player.REPEAT_MODE_OFF,
                onClick = player::cycleRepeat,
                label = {
                    Text(
                        when (state.repeatMode) {
                            Player.REPEAT_MODE_ALL -> "Repeat all"
                            Player.REPEAT_MODE_ONE -> "Repeat one"
                            else -> "Repeat"
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            AssistChip(
                onClick = onClear,
                label = { Text("Clear") },
                leadingIcon = { Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            Text(
                "Up next",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$left left · ${state.queue.size} in queue",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (state.shuffle) "Shuffled · turn off shuffle to reorder" else "Drag to reorder · swipe left to remove",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
}

/** What's playing, over its blurred topic thumbnail, with play/pause. */
@Composable
private fun NowPlayingCard(state: PlayerState, player: PlayerController) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        BlurredCover(
            model = state.coverUri ?: state.talentImageUrl,
            wash = SolidColor(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            modifier = Modifier.matchParentSize(),
            blur = 5.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Artwork(
                state.artworkUri ?: state.talentImageUrl,
                Modifier.size(60.dp).clip(CircleShape).border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                iconSize = 28.dp,
            )
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(state.isPlaying, size = 14.dp)
                    Text(
                        if (state.isPlaying) "  NOW PLAYING" else "  PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOf(state.subtitle, state.album).filter { it.isNotEmpty() && it != state.title }.distinct()
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledIconButton(onClick = player::togglePlayPause, modifier = Modifier.size(48.dp)) {
                PlayPauseIcon(state.isPlaying, Modifier.size(28.dp))
            }
        }
    }
}
