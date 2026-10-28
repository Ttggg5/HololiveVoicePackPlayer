package com.ttggg5.holovoice.ui.playlists

import com.ttggg5.holovoice.ui.components.GridDensity
import com.ttggg5.holovoice.ui.components.DensityCrossfade
import com.ttggg5.holovoice.ui.components.gridColumns
import com.ttggg5.holovoice.ui.components.gridSpacing
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.ttggg5.holovoice.data.PlaylistWithCount
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.pinchToResize
import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * Playlists as a grid of cards: a pinned Favorites card, then each playlist with a mosaic of its
 * topics' thumbnails, a play button, its size and length. Long-press a card for shuffle / rename / delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(viewModel: PlaylistsViewModel, player: PlayerController, onOpen: (Long) -> Unit, onOpenFavorites: () -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val covers by viewModel.covers.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val prefs = LocalContext.current.appContainer.uiPrefs
    val sort by prefs.playlistSort.collectAsStateWithLifecycle()
    val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
    val undo = LocalUndo.current
    var creating by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistWithCount?>(null) }

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
        val list = playlists?.let(sort::sorted) ?: return@Scaffold
        val density = GridDensity.of(cellDp)
        LazyVerticalGrid(
            columns = gridColumns(cellDp, density, scale = 0.85f),
            modifier = Modifier.fillMaxSize().padding(padding).pinchToResize(prefs),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing(density)),
            verticalArrangement = Arrangement.spacedBy(if (density == GridDensity.NORMAL) 14.dp else gridSpacing(density)),
        ) {
            item(key = "favorites") {
                PlaylistCard(
                    density = density,
                    name = "Favorites",
                    details = "$favoriteCount clips",
                    art = {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)),
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                        }
                    },
                    onClick = onOpenFavorites,
                    onPlay = null,
                    modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                )
            }
            items(list, key = { it.playlist.id }) { p ->
                var menu by remember { mutableStateOf(false) }
                Box(Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                    PlaylistCard(
                        density = density,
                        name = p.playlist.name,
                        details = listOf("${p.entryCount} clips", formatTotal(p.totalMs)).filter { it.isNotEmpty() }.joinToString(" · "),
                        art = { PlaylistArt(covers[p.playlist.id].orEmpty(), Modifier.fillMaxSize()) },
                        onClick = { onOpen(p.playlist.id) },
                        onLongClick = { menu = true },
                        onPlay = if (p.entryCount > 0) {
                            { viewModel.withClips(p.playlist.id) { player.playQueue(it, shuffle = false) } }
                        } else {
                            null
                        },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Shuffle") },
                            leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                            enabled = p.entryCount > 0,
                            onClick = {
                                menu = false
                                viewModel.withClips(p.playlist.id) { player.playQueue(it, shuffle = true) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menu = false
                                renaming = p
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menu = false
                                viewModel.delete(p.playlist.id, p.playlist.name) { restore ->
                                    undo?.show("Deleted playlist \"${p.playlist.name}\"", restore)
                                }
                            },
                        )
                    }
                }
            }
            if (list.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyMessage(
                        "No playlists yet",
                        "Mix clips from any topic. Create one with the button below, or long-press any clip and choose \"Add to playlist\".",
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        fill = false,
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
    renaming?.let { p ->
        NameDialog(
            title = "Rename playlist",
            initial = p.playlist.name,
            confirmLabel = "Save",
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                viewModel.rename(p.playlist.id, name)
            },
        )
    }
}

/** A square picture with a round play button on its corner, then the name and details. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    density: GridDensity,
    name: String,
    details: String,
    art: @Composable () -> Unit,
    onClick: () -> Unit,
    onPlay: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    DensityCrossfade(density, modifier) { d -> PlaylistCardBody(d, name, details, art, onClick, onPlay, Modifier, onLongClick) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCardBody(
    density: GridDensity,
    name: String,
    details: String,
    art: @Composable () -> Unit,
    onClick: () -> Unit,
    onPlay: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val longClick: (() -> Unit)? = onLongClick?.let {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            it()
        }
    }
    when (density) {
        // Smallest zoom: just the mosaic.
        GridDensity.MOSAIC -> {
            Box(
                modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = onClick, onLongClick = longClick)
                    .semantics { contentDescription = name },
            ) { art() }
            return
        }
        // Biggest zoom: the mosaic on the left, name, details and play on the right.
        GridDensity.WIDE -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).combinedClickable(onClick = onClick, onLongClick = longClick),
            ) {
                Row(Modifier.height(120.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(120.dp)) { art() }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (details.isNotEmpty()) {
                            Text(
                                details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    if (onPlay != null) {
                        FilledIconButton(onClick = onPlay, modifier = Modifier.padding(end = 14.dp).size(48.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play $name", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            return
        }
        GridDensity.NORMAL -> Unit
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(Modifier.fillMaxSize().shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))) { art() }
            if (onPlay != null) {
                FilledIconButton(
                    onClick = onPlay,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(48.dp).shadow(6.dp, CircleShape),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play $name", modifier = Modifier.size(28.dp))
                }
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
        if (details.isNotEmpty()) {
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
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
