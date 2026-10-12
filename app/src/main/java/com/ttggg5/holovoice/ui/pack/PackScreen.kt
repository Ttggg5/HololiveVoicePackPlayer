package com.ttggg5.holovoice.ui.pack

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.ClipGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackScreen(viewModel: PackViewModel, player: PlayerController, onBack: () -> Unit) {
    val pack by viewModel.pack.collectAsStateWithLifecycle()
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(pack?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { player.playQueue(clips, shuffle = false) }, enabled = clips.isNotEmpty()) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play all")
                    }
                    IconButton(onClick = { player.playQueue(clips, shuffle = true) }, enabled = clips.isNotEmpty()) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle all")
                    }
                },
            )
        },
    ) { padding ->
        ClipGrid(
            clips = clips,
            playerState = playerState,
            showPack = false,
            player = player,
            onToggleFavorite = viewModel::toggleFavorite,
            modifier = Modifier.padding(padding),
        )
    }
}
