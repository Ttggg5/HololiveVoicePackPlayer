package com.ttggg5.holovoice.ui.pack

import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import com.ttggg5.holovoice.ui.library.EditPackDialog
import com.ttggg5.holovoice.ui.components.TalentPickerDialog
import com.ttggg5.holovoice.ui.components.LocalUndo
import androidx.compose.runtime.remember
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.ClipTalent
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.ClipArt
import com.ttggg5.holovoice.ui.components.ClipBrowser
import com.ttggg5.holovoice.ui.playlists.formatTotal
import com.ttggg5.holovoice.ui.talents.StatTile

/**
 * A topic: a header (blurred cover banner, cover, name, talents, stats, play buttons and category
 * filters), then its clips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackScreen(viewModel: PackViewModel, player: PlayerController, onBack: () -> Unit, onOpenTalent: (String) -> Unit) {
    val pack by viewModel.pack.collectAsStateWithLifecycle()
    val allClips by viewModel.clips.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val undo = LocalUndo.current
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var pickTalents by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Forget a category that no longer exists (e.g. after a re-scan).
    val selected = category?.takeIf { it in categories }
    val clips = if (selected == null) allClips else allClips.filter { it.clip.category == selected }
    val talents = allClips.flatMap { it.orderedTalents }.distinctBy { it.slug }
    val name = pack?.name.orEmpty()

    // The top bar is see-through over the banner and fills in once the list scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    if (scrollBehavior.state.overlappedFraction > 0.5f) {
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }, enabled = pack != null) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Topic options")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Play topic next") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                enabled = allClips.isNotEmpty(),
                                onClick = {
                                    menu = false
                                    player.playNext(allClips)
                                    undo?.show("${allClips.size} clips will play next")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Add topic to queue") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                enabled = allClips.isNotEmpty(),
                                onClick = {
                                    menu = false
                                    player.addToQueue(allClips)
                                    undo?.show("Added ${allClips.size} clips to the queue")
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Edit topic") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    editing = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Set talents for all clips") },
                                leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    pickTalents = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Match talents automatically") },
                                leadingIcon = { Icon(Icons.Filled.Face, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    viewModel.matchTalents { undo?.show(it) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete topic", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menu = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        ClipBrowser(
            clips = clips,
            playerState = playerState,
            player = player,
            onToggleFavorite = viewModel::toggleFavorite,
            // The header starts under the top bar so the banner reaches the top of the screen.
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            showPack = false,
            artwork = ClipArt.TALENT,
            showCategory = selected == null && categories.isNotEmpty(),
            emptyTitle = "No clips in this topic",
            emptyBody = "Add audio files to the folder; they appear the next time you open the app.",
            header = {
                PackHeader(
                    cover = pack?.coverUri,
                    name = name,
                    talents = talents,
                    onOpenTalent = onOpenTalent,
                    clipCount = allClips.size,
                    categoryCount = categories.size,
                    totalMs = allClips.sumOf { it.clip.durationMs ?: 0L },
                    plays = allClips.sumOf { it.clip.playCount },
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
                        // The whole topic, in order, after what's already queued (starts it when nothing plays).
                        FilledTonalIconButton(
                            onClick = {
                                player.addToQueue(allClips)
                                undo?.show("Added ${allClips.size} clips to the queue")
                            },
                            enabled = allClips.isNotEmpty(),
                            modifier = Modifier.size(48.dp, 40.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Add topic to queue")
                        }
                    }
                    if (categories.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        ) {
                            FilterChip(selected = selected == null, onClick = { category = null }, label = { Text("All") })
                            categories.forEach { c ->
                                FilterChip(
                                    selected = selected == c,
                                    onClick = { category = if (selected == c) null else c },
                                    label = { Text(c, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    pack?.let { p ->
        if (editing) {
            EditPackDialog(
                pack = p,
                onDismiss = { editing = false },
                onSave = { name, talent ->
                    viewModel.rename(p, name, talent)
                    editing = false
                },
                onDelete = {
                    editing = false
                    confirmDelete = true
                },
                onSetTalents = { talents ->
                    viewModel.setTalents(p, talents) { undo?.show(it) }
                    editing = false
                },
            )
        }
        if (pickTalents) {
            TalentPickerDialog(
                title = "Talents for all clips",
                initialSlugs = emptyList(),
                onConfirm = { talents ->
                    pickTalents = false
                    viewModel.setTalents(p, talents) { undo?.show(it) }
                },
                onDismiss = { pickTalents = false },
            )
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete ${p.name}?") },
                text = {
                    Text(
                        "The topic is removed from the library. Files imported from a ZIP are deleted; " +
                            "files in a picked folder are left untouched.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        viewModel.delete(p) {
                            undo?.show("Deleted ${p.name}")
                            onBack()
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun PackHeader(
    cover: String?,
    name: String,
    talents: List<ClipTalent>,
    onOpenTalent: (String) -> Unit,
    clipCount: Int,
    categoryCount: Int,
    totalMs: Long,
    plays: Int,
    actions: @Composable () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    // Reaches past the list's side and top padding so the banner runs edge to edge.
    Box(
        Modifier.fillMaxWidth().layout { measurable, constraints ->
            val side = 12.dp.roundToPx()
            val top = 4.dp.roundToPx()
            val wide = constraints.maxWidth + side * 2
            val placeable = measurable.measure(constraints.copy(minWidth = wide, maxWidth = wide))
            layout(constraints.maxWidth, (placeable.height - top).coerceAtLeast(0)) { placeable.place(-side, -top) }
        },
    ) {
        // Banner: the topic's cover, softly blurred, fading into the page.
        BlurredCover(
            model = cover,
            wash = Brush.verticalGradient(
                0f to surface.copy(alpha = 0.3f),
                0.6f to surface.copy(alpha = 0.55f),
                1f to surface,
            ),
            modifier = Modifier.fillMaxWidth().height(230.dp).background(MaterialTheme.colorScheme.surfaceContainer),
            blur = 4.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 104.dp)) {
            Artwork(
                cover,
                Modifier
                    .size(148.dp)
                    .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = primary, spotColor = primary)
                    .clip(RoundedCornerShape(24.dp)),
                iconSize = 56.dp,
                placeholder = Icons.Filled.LibraryMusic,
            )
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 14.dp, start = 16.dp, end = 16.dp),
            )
            if (talents.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    talents.forEach { t -> TalentPill(t) { onOpenTalent(t.slug) } }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                StatTile("$clipCount", "clips", Modifier.weight(1f))
                if (categoryCount > 0) StatTile("$categoryCount", "categories", Modifier.weight(1f))
                formatTotal(totalMs).takeIf { it.isNotEmpty() }?.let { StatTile(it, "total", Modifier.weight(1f)) }
                StatTile("$plays", "plays", Modifier.weight(1f))
            }
            actions()
        }
    }
}

/** A talent in this topic: picture and name; tap to open their page. */
@Composable
private fun TalentPill(talent: ClipTalent, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Artwork(talent.imageUrl, Modifier.size(28.dp).clip(CircleShape), iconSize = 16.dp)
        Text(
            TalentTags.clean(talent.name),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
