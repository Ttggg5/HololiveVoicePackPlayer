package com.ttggg5.holovoice.ui.playlists

import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.PlaylistSort
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.SortMenu
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.EmptyMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(viewModel: PlaylistsViewModel, player: PlayerController, onOpen: (Long) -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val prefs = LocalContext.current.appContainer.uiPrefs
    val sort by prefs.playlistSort.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                actions = { SortMenu(PlaylistSort.entries, sort, { it.label }, prefs::setPlaylistSort) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New playlist") },
            )
        },
    ) { padding ->
        val list = playlists?.let(sort::sorted)
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyMessage(
                "No playlists yet",
                "Mix clips from any topic. Create one here, or long-press any clip and choose \"Add to playlist\".",
                Modifier.padding(padding),
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                actionLabel = "New playlist",
                onAction = { creating = true },
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(list, key = { it.playlist.id }) { p ->
                    ListItem(
                        modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()).clickable { onOpen(p.playlist.id) },
                        headlineContent = { Text(p.playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${p.entryCount} clips") },
                        leadingContent = {
                            Artwork(
                                p.coverUri,
                                Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                                iconSize = 24.dp,
                                placeholder = Icons.AutoMirrored.Filled.QueueMusic,
                            )
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { viewModel.withClips(p.playlist.id) { player.playQueue(it, shuffle = false) } },
                                enabled = p.entryCount > 0,
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${p.playlist.name}")
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                viewModel.create(name, onCreated = onOpen)
            },
        )
    }
}

@Composable
fun NameDialog(title: String, initial: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
