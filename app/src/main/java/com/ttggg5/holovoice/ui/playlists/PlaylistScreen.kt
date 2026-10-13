package com.ttggg5.holovoice.ui.playlists

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.thumbnail
import com.ttggg5.holovoice.data.speaker
import androidx.compose.ui.platform.LocalContext
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.LocalUndo
import kotlinx.coroutines.launch
import com.ttggg5.holovoice.ui.components.ClipActionsSheet
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.TrackList
import com.ttggg5.holovoice.ui.components.TrackRow
import com.ttggg5.holovoice.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(viewModel: PlaylistViewModel, player: PlayerController, onBack: () -> Unit) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    val clips = items.orEmpty().map { it.item }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<ClipWithPack?>(null) }
    val undo = LocalUndo.current
    val appScope = LocalContext.current.appContainer.appScope

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Add all to queue") }, enabled = clips.isNotEmpty(), onClick = {
                            menuOpen = false
                            player.addToQueue(clips)
                        })
                        DropdownMenuItem(text = { Text("Rename") }, onClick = {
                            menuOpen = false
                            renaming = true
                        })
                        DropdownMenuItem(text = { Text("Delete playlist") }, onClick = {
                            menuOpen = false
                            confirmDelete = true
                        })
                    }
                },
            )
        },
    ) { padding ->
        val list = items
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyMessage(
                "This playlist is empty",
                "Long-press any clip and choose \"Add to playlist\".",
                Modifier.padding(padding),
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            )
            else -> TrackList(
                items = list,
                key = { it.entryId },
                row = { entry ->
                    val current = playerState.clipId == entry.item.clip.id
                    TrackRow(
                        title = entry.item.clip.title,
                        subtitle = listOfNotNull(
                            entry.item.speaker,
                            entry.item.packName,
                            entry.item.clip.durationMs?.let(::formatDuration),
                        ).joinToString(" · "),
                        artwork = entry.item.thumbnail,
                        highlighted = current,
                        playing = current && playerState.isPlaying,
                    )
                },
                onClick = { _, index -> player.playFrom(clips, index) },
                onLongClick = { actionsFor = it.item },
                onRemove = { item ->
                    val index = list.indexOf(item)
                    viewModel.remove(item, index) { undoRemove ->
                        undo?.show("Removed \"${item.item.clip.title}\"", undoRemove)
                    }
                },
                onMove = viewModel::move,
                modifier = Modifier.padding(padding),
            )
        }
    }

    actionsFor?.let { target ->
        val fresh = clips.firstOrNull { it.clip.id == target.clip.id } ?: target
        ClipActionsSheet(fresh, player, viewModel::toggleFavorite, onDismiss = { actionsFor = null })
    }
    if (renaming) {
        NameDialog(
            title = "Rename playlist",
            initial = playlist?.name.orEmpty(),
            confirmLabel = "Save",
            onDismiss = { renaming = false },
            onConfirm = {
                renaming = false
                viewModel.rename(it)
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${playlist?.name.orEmpty()}?") },
            text = { Text("Only the playlist is deleted; the clips stay in your library.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val name = playlist?.name.orEmpty()
                    viewModel.delete { recreate ->
                        onBack()
                        undo?.show("Deleted playlist \"$name\"") { appScope.launch { recreate() } }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
