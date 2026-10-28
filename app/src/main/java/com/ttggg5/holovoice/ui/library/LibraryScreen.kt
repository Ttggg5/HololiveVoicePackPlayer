package com.ttggg5.holovoice.ui.library

import com.ttggg5.holovoice.ui.components.ListenedBadge
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.Pack
import com.ttggg5.holovoice.data.PackWithCount
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
import com.ttggg5.holovoice.ui.components.LibraryRefreshBox
import com.ttggg5.holovoice.ui.components.RefreshAction
import com.ttggg5.holovoice.ui.components.SortMenu
import com.ttggg5.holovoice.ui.components.pinchToResize
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars

/** All topics: a headline with counts, a search bar, what's playing, then cover cards (grouped by talent when sorted that way). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, playerState: PlayerState, onOpenPack: (Long) -> Unit) {
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val packTalents by viewModel.packTalents.collectAsStateWithLifecycle()
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
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!importing) menuOpen = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (importing) "Importing…" else "Import") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().statusBarsPadding()) {
            TopicsHeader(
                topicCount = packs?.size ?: 0,
                clipCount = packs?.sumOf { it.clipCount } ?: 0,
                sort = sort,
                onSort = prefs::setTopicSort,
                busy = importing,
                onMatchTalents = viewModel::matchTalents,
                onRefreshTalents = viewModel::refreshTalents,
            )
            AnimatedVisibility(visible = importing) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            }
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
                    val q = query.trim()
                    val shown = sort.sorted(packs!!).filter { p ->
                        q.isEmpty() || p.pack.name.contains(q, ignoreCase = true) || p.pack.talent.contains(q, ignoreCase = true)
                    }
                    val playing = packs!!.firstOrNull { it.pack.id == playerState.packId }
                    LibraryRefreshBox(Modifier.fillMaxSize()) {
                        PackGrid(
                            packs = shown,
                            groupByTalent = sort == TopicSort.TALENT,
                            packTalents = packTalents,
                            playerState = playerState,
                            onOpen = onOpenPack,
                            onLongPress = { editing = it },
                            showSearch = packs!!.size > 3,
                            query = query,
                            onQueryChange = { query = it },
                            playingFrom = playing,
                            noMatches = q.isNotEmpty() && shown.isEmpty(),
                        )
                    }
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

@Composable
private fun TopicsHeader(
    topicCount: Int,
    clipCount: Int,
    sort: TopicSort,
    onSort: (TopicSort) -> Unit,
    busy: Boolean,
    onMatchTalents: () -> Unit,
    onRefreshTalents: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 4.dp, bottom = 8.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Topics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (topicCount > 0) {
                Text(
                    "$topicCount topics · $clipCount clips",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RefreshAction()
        SortMenu(TopicSort.entries, sort, { it.label }, onSort)
        var overflow by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { overflow = true }, enabled = !busy) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Text("Match talents automatically") },
                    leadingIcon = { Icon(Icons.Filled.Face, contentDescription = null) },
                    onClick = {
                        overflow = false
                        onMatchTalents()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Refresh talent list") },
                    leadingIcon = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                    onClick = {
                        overflow = false
                        onRefreshTalents()
                    },
                )
            }
        }
    }
}

@Composable
private fun TopicSearchField(query: String, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Find a topic or talent") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Slim card for the topic the current clip comes from; tap to open it. */
@Composable
private fun PlayingFromStrip(pack: PackWithCount, isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        BlurredCover(
            model = pack.pack.coverUri,
            wash = SolidColor(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            modifier = Modifier.matchParentSize(),
            blur = 3.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Artwork(pack.pack.coverUri, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), iconSize = 22.dp, placeholder = Icons.Filled.LibraryMusic)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(isPlaying, size = 14.dp)
                    Text(
                        "  Playing from",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    pack.pack.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun PackGrid(
    packs: List<PackWithCount>,
    groupByTalent: Boolean,
    packTalents: Map<Long, List<String>>,
    playerState: PlayerState,
    onOpen: (Long) -> Unit,
    onLongPress: (Pack) -> Unit,
    showSearch: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    playingFrom: PackWithCount?,
    noMatches: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    val prefs = LocalContext.current.appContainer.uiPrefs
    val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
    // Group headers only make sense when the list is sorted by talent.
    val groups: List<Pair<String?, List<PackWithCount>>> =
        if (groupByTalent) packs.groupBy { TalentTags.clean(it.pack.talent).ifBlank { "No talent" } }.toList() else listOf(null to packs)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cellDp.dp),
        modifier = Modifier.pinchToResize(prefs),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showSearch) item(key = "search", span = { GridItemSpan(maxLineSpan) }) { TopicSearchField(query, onQueryChange) }
        if (playingFrom != null) {
            item(key = "playing", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                    PlayingFromStrip(playingFrom, playerState.isPlaying) { onOpen(playingFrom.pack.id) }
                }
            }
        }
        if (noMatches) {
            item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "No topics match",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                )
            }
        }
        groups.forEach { (talent, inGroup) ->
            if (talent != null) {
                item(key = "group:$talent", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        talent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())
                            .padding(top = 8.dp, start = 4.dp),
                    )
                }
            }
            items(inGroup, key = { it.pack.id }) { item ->
                TopicCoverCard(
                    item = item,
                    talents = packTalents[item.pack.id].orEmpty(),
                    playingHere = playerState.packId == item.pack.id,
                    isPlaying = playerState.isPlaying,
                    onClick = { onOpen(item.pack.id) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(item.pack)
                    },
                    modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                )
            }
        }
    }
}

/** A topic as a full-bleed cover with its name, talents and counts on a dark gradient. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopicCoverCard(
    item: PackWithCount,
    talents: List<String>,
    playingHere: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "topicPress")
    val ring by animateColorAsState(
        if (playingHere) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(Motion.MEDIUM),
        label = "topicRing",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .border(3.dp, ring, RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Artwork(item.pack.coverUri, Modifier.fillMaxSize(), iconSize = 48.dp, placeholder = Icons.Filled.LibraryMusic)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
        // Every clip listened to.
        if (item.allListened) ListenedBadge(30.dp, Modifier.align(Alignment.TopStart).padding(10.dp))
        // Badge on the topic that is playing right now.
        AnimatedVisibility(
            visible = playingHere,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                EqualizerBars(
                    playing = isPlaying,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(
                item.pack.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                if (talents.isNotEmpty()) {
                    AvatarStack(talents)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    buildAnnotatedString {
                        append("${item.clipCount} clips")
                        if (item.allListened) {
                            append(" · ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold)) { append("All listened") }
                        } else if (item.categoryCount > 0) {
                            append(" · ${item.categoryCount} categories")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Up to three small talent pictures, overlapping. */
@Composable
private fun AvatarStack(images: List<String>) {
    val size = 22.dp
    val step = 14.dp
    Box(Modifier.width(size + step * (images.size - 1)).height(size)) {
        images.forEachIndexed { i, url ->
            Artwork(
                url,
                Modifier.offset(x = step * i).size(size).clip(CircleShape).border(1.5.dp, Color.White, CircleShape),
                iconSize = 12.dp,
            )
        }
    }
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
