package com.ttggg5.holovoice.ui.favorites

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.ClipGrid
import com.ttggg5.holovoice.ui.components.EmptyMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel, player: PlayerController) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    val clips = favorites.orEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                actions = {
                    IconButton(onClick = { player.playQueue(clips) }, enabled = clips.isNotEmpty()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play all")
                    }
                    IconButton(onClick = { player.playQueue(clips, shuffle = true) }, enabled = clips.isNotEmpty()) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all")
                    }
                },
            )
        },
    ) { padding ->
        if (favorites?.isEmpty() == true) {
            EmptyMessage(
                title = "No favorites yet",
                body = "Tap the star on any clip to collect it here.",
                modifier = Modifier.padding(padding),
            )
        } else {
            ClipGrid(
                clips = clips,
                playerState = playerState,
                showPack = true,
                onPlay = player::playClip,
                onToggleFavorite = viewModel::toggleFavorite,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
