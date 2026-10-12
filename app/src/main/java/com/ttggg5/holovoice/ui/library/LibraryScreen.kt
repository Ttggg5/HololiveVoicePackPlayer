package com.ttggg5.holovoice.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ttggg5.holovoice.data.Pack
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.talents.Talent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.vector.ImageVector
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.TopicSort
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.SortMenu
import com.ttggg5.holovoice.ui.components.pinchToResize
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.TalentPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, playerState: PlayerState, onOpenPack: (Long) -> Unit) {
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pack?>(null) }
    val prefs = LocalContext.current.appContainer.uiPrefs
    val sort by prefs.topicSort.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::importFolder)
    }
    val pickLibrary = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::importLibrary)
    }
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importZip)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Topics") },
                actions = {
                    SortMenu(TopicSort.entries, sort, { it.label }, prefs::setTopicSort)
                    var overflow by remember { mutableStateOf(false) }
                    IconButton(onClick = { overflow = true }, enabled = !importing) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                        DropdownMenuItem(
                            text = { Text("Match talents automatically") },
                            leadingIcon = { Icon(Icons.Filled.Face, contentDescription = null) },
                            onClick = {
                                overflow = false
                                viewModel.matchTalents()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh talent list") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            onClick = {
                                overflow = false
                                viewModel.refreshTalents()
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!importing) menuOpen = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (importing) "Importing…" else "Import") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                packs == null -> Unit
                packs!!.isEmpty() -> EmptyMessage(
                    title = "No topics yet",
                    body = "Pick the folder that holds all your voice packs — each sub-folder becomes a topic " +
                        "(mp3, m4a, ogg, opus, wav or flac). Or use Import for a single folder or a .zip. " +
                        "Imported folders stay in sync automatically.",
                    icon = Icons.Filled.LibraryMusic,
                    actionLabel = "Import many topics",
                    onAction = { pickLibrary.launch(null) },
                )
                else -> {
                    val shown = sort.sorted(packs!!).filter { p ->
                        query.isBlank() || p.pack.name.contains(query.trim(), ignoreCase = true) ||
                            p.pack.talent.contains(query.trim(), ignoreCase = true)
                    }
                    if (packs!!.size > 3) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Find a topic") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    PackGrid(shown, playerState, onOpenPack, onLongPress = { editing = it })
                }
            }
        }
    }

    if (menuOpen) {
        ImportSheet(
            onDismiss = { menuOpen = false },
            onImportMany = {
                menuOpen = false
                pickLibrary.launch(null)
            },
            onImportOne = {
                menuOpen = false
                pickFolder.launch(null)
            },
            onImportZip = {
                menuOpen = false
                pickZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            },
        )
    }

    editing?.let { pack ->
        EditPackDialog(
            pack = pack,
            onDismiss = { editing = null },
            onSave = { name, talent ->
                viewModel.rename(pack, name, talent)
                editing = null
            },
            onDelete = {
                viewModel.delete(pack)
                editing = null
            },
            onSetTalents = { talents ->
                viewModel.setPackTalents(pack, talents)
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PackGrid(
    packs: List<PackWithCount>,
    playerState: PlayerState,
    onOpen: (Long) -> Unit,
    onLongPress: (Pack) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val prefs = LocalContext.current.appContainer.uiPrefs
    val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cellDp.dp),
        modifier = Modifier.pinchToResize(prefs),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        run {
            val talentPacks = packs
            items(talentPacks, key = { it.pack.id }) { item ->
                val playingHere = playerState.packId == item.pack.id
                Card(
                    Modifier.animateItem().fillMaxWidth().combinedClickable(
                        onClick = { onOpen(item.pack.id) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress(item.pack)
                        },
                    ),
                ) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                        if (item.pack.coverUri != null) {
                            AsyncImage(
                                model = item.pack.coverUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        // Badge on the topic that is playing right now.
                        androidx.compose.animation.AnimatedVisibility(
                            visible = playingHere,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                                EqualizerBars(
                                    playing = playerState.isPlaying,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(6.dp),
                                )
                            }
                        }
                    }
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            item.pack.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${item.pack.talent} · ${item.clipCount} clips" +
                                if (item.categoryCount > 0) " · ${item.categoryCount} categories" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPackDialog(
    pack: Pack,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onSetTalents: (List<Talent>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(pack.name) }
    var talent by rememberSaveable { mutableStateOf(pack.talent) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickTalent by remember { mutableStateOf(false) }

    if (pickTalent) {
        TalentPickerDialog(
            title = "Talents for all clips",
            initialSlugs = emptyList(),
            onConfirm = onSetTalents,
            onDismiss = { pickTalent = false },
        )
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${pack.name}?") },
            text = {
                Text(
                    "The topic is removed from the library. Files imported from a ZIP are deleted; " +
                        "files in a picked folder are left untouched.",
                )
            },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = talent, onValueChange = { talent = it }, label = { Text("Talent") }, singleLine = true)
                TextButton(onClick = { pickTalent = true }) {
                    Text("Set talents for all clips…")
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete topic", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, talent) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** "Add voice packs" bottom sheet with one roomy card per import mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(onDismiss: () -> Unit, onImportMany: () -> Unit, onImportOne: () -> Unit, onImportZip: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text("Add voice packs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
            ImportOption(
                icon = Icons.Filled.CreateNewFolder,
                title = "Import many topics",
                body = "Pick the folder that holds all your packs. Each sub-folder becomes its own topic and new ones are added automatically.",
                highlighted = true,
                onClick = onImportMany,
            )
            ImportOption(
                icon = Icons.Filled.Folder,
                title = "Import one topic",
                body = "Pick a single folder. It becomes one topic; its sub-folders become categories.",
                onClick = onImportOne,
            )
            ImportOption(
                icon = Icons.Filled.FolderZip,
                title = "Import ZIP",
                body = "Extract a .zip into the app. You can delete the ZIP afterwards.",
                onClick = onImportZip,
            )
        }
    }
}

@Composable
private fun ImportOption(icon: ImageVector, title: String, body: String, onClick: () -> Unit, highlighted: Boolean = false) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
