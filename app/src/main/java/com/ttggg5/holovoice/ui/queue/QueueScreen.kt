package com.ttggg5.holovoice.ui.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.TrackList
import com.ttggg5.holovoice.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(player: PlayerController, onBack: () -> Unit) {
    val state by player.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Start with the current item in view.
    LaunchedEffect(Unit) {
        val current = state.queue.indexOfFirst { it.index == state.currentIndex }
        if (current > 0) listState.scrollToItem(current - 1)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Up next") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = player::clearQueue, enabled = state.queue.isNotEmpty()) {
                        Icon(Icons.Filled.ClearAll, contentDescription = "Clear queue")
                    }
                },
            )
        },
    ) { padding ->
        if (state.queue.isEmpty()) {
            EmptyMessage("The queue is empty", "Play a pack, playlist or clip to fill it.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding)) {
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
                onRemove = { player.removeQueueItem(it.index) },
                // Without shuffle the play order is the item order, so list positions map to player indices.
                onMove = if (state.shuffle) null else { from, to -> player.moveQueueItem(from, to) },
                listState = listState,
            )
        }
    }
}
