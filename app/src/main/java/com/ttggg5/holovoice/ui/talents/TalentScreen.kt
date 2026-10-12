package com.ttggg5.holovoice.ui.talents

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.ClipBrowser

/** All clips of one talent across every topic, with a profile header. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentScreen(viewModel: TalentViewModel, player: PlayerController, onBack: () -> Unit) {
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val talent by viewModel.talent.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    var topicFilter by rememberSaveable { mutableStateOf<Long?>(null) }

    val all = clips.orEmpty()
    val topics = all.map { it.clip.packId to it.packName }.distinct()
    val shown = if (topicFilter == null) all else all.filter { it.clip.packId == topicFilter }
    val name = summary?.name ?: talent?.nameEn ?: if (viewModel.slug == null) "No talent yet" else ""

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (clips != null) {
            ClipBrowser(
                clips = shown,
                playerState = playerState,
                player = player,
                onToggleFavorite = viewModel::toggleFavorite,
                modifier = Modifier.padding(padding),
                emptyTitle = "No clips",
                header = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Artwork(
                            summary?.imageUrl ?: talent?.imageUrl,
                            Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            iconSize = 56.dp,
                            placeholder = if (viewModel.slug == null) Icons.Filled.PersonOff else Icons.Filled.Face,
                        )
                        Text(name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                        talent?.nameJa?.let {
                            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${all.size} clips · ${topics.size} topics" + (summary?.plays?.takeIf { it > 0 }?.let { " · $it plays" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                            Button(onClick = { player.playQueue(shown, shuffle = false) }, enabled = shown.isNotEmpty()) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(" Play all")
                            }
                            FilledTonalButton(onClick = { player.playQueue(shown, shuffle = true) }, enabled = shown.isNotEmpty()) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                Text(" Shuffle")
                            }
                        }
                        if (topics.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            ) {
                                FilterChip(selected = topicFilter == null, onClick = { topicFilter = null }, label = { Text("All topics") })
                                topics.forEach { (id, topicName) ->
                                    FilterChip(
                                        selected = topicFilter == id,
                                        onClick = { topicFilter = if (topicFilter == id) null else id },
                                        label = { Text(topicName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}
