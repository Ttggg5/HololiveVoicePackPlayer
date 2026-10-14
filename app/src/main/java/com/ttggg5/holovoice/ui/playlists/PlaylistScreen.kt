package com.ttggg5.holovoice.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.ui.components.BlurredCover
import androidx.compose.foundation.layout.WindowInsets
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
import com.ttggg5.holovoice.data.artwork
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
    val covers by viewModel.covers.collectAsStateWithLifecycle()
    val clips = items.orEmpty().map { it.item }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<ClipWithPack?>(null) }
    val undo = LocalUndo.current
    val appScope = LocalContext.current.appContainer.appScope

    // The top bar is see-through over the banner and fills in once the list scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    if (scrollBehavior.state.overlappedFraction > 0.5f) {
                        Text(playlist?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
        val list = items ?: return@Scaffold
        val header: @Composable () -> Unit = {
            PlaylistHeader(
                name = playlist?.name.orEmpty(),
                covers = covers,
                details = listOf(
                    "${clips.size} clips",
                    formatTotal(clips.sumOf { it.clip.durationMs ?: 0L }),
                    clips.flatMap { it.orderedTalents }.distinctBy { it.slug }.size.takeIf { it > 0 }?.let { "$it talents" }.orEmpty(),
                ).filter { it.isNotEmpty() }.joinToString(" · "),
                onRename = { renaming = true },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(onClick = { player.playQueue(clips, shuffle = false) }, enabled = clips.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(" Play all")
                    }
                    FilledTonalButton(onClick = { player.playQueue(clips, shuffle = true) }, enabled = clips.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                        Text(" Shuffle")
                    }
                }
            }
        }
        if (list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                header()
                EmptyMessage(
                    "This playlist is empty",
                    "Long-press any clip and choose \"Add to playlist\".",
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    fill = false,
                )
            }
        } else {
            TrackList(
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
                        artwork = entry.item.artwork,
                        badge = entry.item.coverUri.takeIf { it != entry.item.artwork },
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
                // The header starts under the top bar so the banner reaches the top of the screen.
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                contentPadding = PaddingValues(bottom = 16.dp),
                header = header,
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

/** Blurred banner of the first cover, the playlist's mosaic, its name (tap to rename) and details. */
@Composable
private fun PlaylistHeader(name: String, covers: List<String>, details: String, onRename: () -> Unit, actions: @Composable () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    Box(Modifier.fillMaxWidth()) {
        BlurredCover(
            model = covers.firstOrNull(),
            wash = Brush.verticalGradient(
                0f to surface.copy(alpha = 0.35f),
                0.6f to surface.copy(alpha = 0.6f),
                1f to surface,
            ),
            modifier = Modifier.fillMaxWidth().height(240.dp).background(MaterialTheme.colorScheme.surfaceContainer),
            blur = 4.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 96.dp)) {
            PlaylistArt(
                covers,
                Modifier
                    .size(170.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                iconSize = 64.dp,
            )
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 14.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRename),
            )
            if (details.isNotEmpty()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            actions()
        }
    }
}
