package com.ttggg5.holovoice.ui.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.TrackList
import com.ttggg5.holovoice.ui.components.TrackRow

/**
 * The play queue: tap to jump, drag to reorder, swipe to remove (with Undo). Used by the Queue
 * screen, the swipe-up sheet in the player, and the player's side panel on large screens.
 */
@Composable
fun QueueList(player: PlayerController, modifier: Modifier = Modifier) {
    val state by player.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val undo = LocalUndo.current

    // Start with the current item in view.
    LaunchedEffect(Unit) {
        val current = state.queue.indexOfFirst { it.index == state.currentIndex }
        if (current > 0) listState.scrollToItem(current - 1)
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
    Column(modifier) {
        Text(
            text = "${state.queue.size} clips" +
                if (state.shuffle) " · shuffled (turn off shuffle to reorder)" else " · drag to reorder, swipe to remove",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TrackList(
            items = state.queue,
            key = { it.key },
            row = { item ->
                val current = item.index == state.currentIndex
                TrackRow(item.title, item.subtitle, item.artworkUri, highlighted = current, playing = current && state.isPlaying)
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
        )
    }
}
