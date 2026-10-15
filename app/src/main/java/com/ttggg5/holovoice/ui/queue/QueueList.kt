package com.ttggg5.holovoice.ui.queue

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.ttggg5.holovoice.data.moved
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.playback.QueueItem
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.components.PlayPauseIcon
import com.ttggg5.holovoice.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** How long a swiped row stays hidden while the player removes it (it comes back if that failed). */
private const val REMOVE_HIDE_MS = 2_000L

/**
 * The play queue in sections: shuffle / repeat / clear, NOW PLAYING, UP NEXT (cards: tap to
 * jump, ⋮ for more, drag the handle to reorder, swipe left to remove with Undo) and a collapsible
 * PLAYED list. Used by the Queue screen, the swipe-up panel in the player, and the player's side
 * panel on large screens. Rows act on their queue key, looked up when the action runs, so quick
 * swipes always hit the clip that was swiped.
 */
@Composable
fun QueueList(player: PlayerController, modifier: Modifier = Modifier, showNowPlaying: Boolean = false) {
    val state by player.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val undo = LocalUndo.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var showPlayed by rememberSaveable { mutableStateOf(false) }

    if (state.queue.isEmpty()) {
        EmptyMessage(
            "The queue is empty",
            "Play a topic, talent, playlist or clip to fill it.",
            modifier,
            icon = Icons.Filled.ClearAll,
        )
        return
    }

    // Rows swiped away, hidden right away until the player's queue catches up.
    val hidden = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(state.queue) {
        val keys = state.queue.mapTo(HashSet()) { it.key }
        hidden.keys.filter { it !in keys }.forEach { hidden.remove(it) }
    }
    fun remove(item: QueueItem) {
        if (hidden[item.key] == true) return
        hidden[item.key] = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        player.removeQueueItem(item.key) { index, removed ->
            undo?.show("Removed \"${item.title}\" from the queue") { player.restoreQueueItem(index, removed) }
        }
        scope.launch {
            delay(REMOVE_HIDE_MS)
            hidden.remove(item.key)
        }
    }

    val queue = state.queue.filterNot { hidden[it.key] == true }
    val currentPos = queue.indexOfFirst { it.index == state.currentIndex }
    val current = queue.getOrNull(currentPos)
    val played = if (currentPos > 0) queue.subList(0, currentPos) else emptyList()
    val upcoming = if (currentPos >= 0) queue.drop(currentPos + 1) else queue

    // Local copy of Up next so a dragged card follows the finger; synced from the player otherwise.
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableStateOf(upcoming) }
    val latestUpcoming by rememberUpdatedState(upcoming)
    LaunchedEffect(upcoming) { if (!dragging) local = upcoming }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val keys = local.map { upKey(it) }
        val a = keys.indexOf(from.key)
        val b = keys.indexOf(to.key)
        // Only Up next cards move, and only among themselves.
        if (a >= 0 && b >= 0) local = local.moved(a, b)
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "controls") {
            Column {
                if (showNowPlaying && state.clipId != null) NowPlayingCard(state, player)
                Controls(state, player, left = upcoming.size, onClear = { confirmClear = true })
            }
        }
        if (current != null && !showNowPlaying) {
            item(key = "nowTitle") { SectionTitle("Now playing") }
            item(key = "now:" + current.key) {
                NowRow(current, state.isPlaying, onClick = player::togglePlayPause, modifier = Modifier.animateItem())
            }
        }
        item(key = "nextTitle") {
            SectionTitle(
                "Up next",
                count = upcoming.size,
                hint = when {
                    upcoming.isEmpty() -> null
                    state.shuffle -> "Shuffled · turn off shuffle to reorder"
                    else -> "Drag ≡ to reorder · swipe left to remove"
                },
            )
        }
        if (upcoming.isEmpty()) {
            item(key = "nothingNext") {
                Text(
                    if (state.repeatMode == Player.REPEAT_MODE_OFF) "Nothing up next. Playback stops after this clip." else "Nothing up next. The queue repeats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        items(local, key = { upKey(it) }) { item ->
            ReorderableItem(reorderState, key = upKey(item)) { isDragging ->
                val handle: (@Composable () -> Unit)? = if (state.shuffle) null else {
                    {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragging = true
                                },
                                onDragStopped = {
                                    val to = local.indexOfFirst { it.key == item.key }
                                    val target = latestUpcoming.getOrNull(to)
                                    if (target != null && target.key != item.key) player.moveQueueItem(item.key, target.key)
                                    dragging = false
                                },
                            ),
                        ) {
                            Icon(Icons.Filled.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                SwipeRow(onRemove = { remove(item) }) {
                    QueueRow(
                        item = item,
                        lifted = isDragging,
                        onClick = { player.skipTo(item.key) },
                        menu = { close ->
                            DropdownMenuItem(
                                text = { Text("Play now") },
                                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                onClick = {
                                    close()
                                    player.skipTo(item.key)
                                },
                            )
                            if (!state.shuffle && local.firstOrNull()?.key != item.key) {
                                DropdownMenuItem(
                                    text = { Text("Play next") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                    onClick = {
                                        close()
                                        player.playNext(item.key)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Remove from queue") },
                                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    close()
                                    remove(item)
                                },
                            )
                        },
                        handle = handle,
                    )
                }
            }
        }
        if (played.isNotEmpty()) {
            item(key = "playedTitle") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable { showPlayed = !showPlayed }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        "PLAYED · ${played.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (showPlayed) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showPlayed) "Hide played" else "Show played",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showPlayed) {
                items(played, key = { "played:" + it.key }) { item ->
                    Box(Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                        SwipeRow(onRemove = { remove(item) }) {
                            QueueRow(
                                item = item,
                                faded = true,
                                onClick = { player.skipTo(item.key) },
                                menu = { close ->
                                    DropdownMenuItem(
                                        text = { Text("Play again") },
                                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                        onClick = {
                                            close()
                                            player.skipTo(item.key)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove from queue") },
                                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                                        onClick = {
                                            close()
                                            remove(item)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the queue?") },
            text = { Text("Playback stops and all ${state.queue.size} clips are removed from the queue.") },
            confirmButton = {
                TextButton(onClick = {
                    player.clearQueue()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

/** The clip's sequence number from its file name as a small "#n" badge (as on the topic page), then who · topic. */
@Composable
private fun Details(item: QueueItem) {
    if (item.trackNo == null && item.subtitle.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        item.trackNo?.let { n ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier.padding(end = 6.dp),
            ) {
                Text(
                    "#$n",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        if (item.subtitle.isNotEmpty()) {
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Lazy-list key of an Up next card. */
private fun upKey(item: QueueItem) = "next:" + item.key

@Composable
private fun Controls(state: PlayerState, player: PlayerController, left: Int, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = state.shuffle,
                onClick = player::toggleShuffle,
                label = { Text("Shuffle") },
                leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = state.repeatMode != Player.REPEAT_MODE_OFF,
                onClick = player::cycleRepeat,
                label = {
                    Text(
                        when (state.repeatMode) {
                            Player.REPEAT_MODE_ALL -> "Repeat all"
                            Player.REPEAT_MODE_ONE -> "Repeat one"
                            else -> "Repeat"
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            AssistChip(
                onClick = onClear,
                label = { Text("Clear") },
                leadingIcon = { Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        Text(
            "$left left · ${state.queue.size} total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: Int? = null, hint: String? = null) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
        Text(
            title.uppercase() + (count?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The talent's round picture with the topic thumbnail on its corner. */
@Composable
private fun QueueArt(item: QueueItem, size: Int) {
    val badge = item.coverUri.takeIf { item.artworkUri != null && it != item.artworkUri.toString() }
    Box(Modifier.size((size + 4).dp)) {
        Artwork(item.artworkUri ?: item.coverUri, Modifier.size(size.dp).clip(CircleShape), iconSize = (size / 2).dp)
        if (badge != null) {
            Artwork(
                badge,
                Modifier
                    .align(Alignment.BottomEnd)
                    .size((size / 2).dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp)),
                iconSize = 10.dp,
            )
        }
    }
}

/** The playing clip, large; tap to play or pause. */
@Composable
private fun NowRow(item: QueueItem, isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            QueueArt(item, 56)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Details(item)
            }
            EqualizerBars(isPlaying, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
        }
    }
}

/** Swipe left to remove: a rounded red pill shows behind the card while swiping. */
@Composable
private fun SwipeRow(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val currentRemove by rememberUpdatedState(onRemove)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentRemove()
            // Always snap back; the row leaves when the queue updates (it's hidden right away).
            value == SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Remove", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onError)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        },
    ) { content() }
}

/** One clip as a rounded card: picture, title, #n · who · topic, ⋮ and (optionally) a drag handle. */
@Composable
private fun QueueRow(
    item: QueueItem,
    onClick: () -> Unit,
    menu: @Composable (close: () -> Unit) -> Unit,
    lifted: Boolean = false,
    faded: Boolean = false,
    handle: (@Composable () -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (faded) 0.55f else 1f, label = "queueRowAlpha")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (lifted) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (lifted) 8.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(alpha).padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
            QueueArt(item, 44)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Details(item)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    menu { menuOpen = false }
                }
            }
            if (handle != null) handle() else Spacer(Modifier.width(4.dp))
        }
    }
}

/** What's playing, over its blurred topic thumbnail, with play/pause (the separate Queue page). */
@Composable
private fun NowPlayingCard(state: PlayerState, player: PlayerController) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        BlurredCover(
            model = state.coverUri ?: state.talentImageUrl,
            wash = SolidColor(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            modifier = Modifier.matchParentSize(),
            blur = 3.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Artwork(
                state.artworkUri ?: state.talentImageUrl,
                Modifier.size(60.dp).clip(CircleShape).border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                iconSize = 28.dp,
            )
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(state.isPlaying, size = 14.dp)
                    Text(
                        if (state.isPlaying) "  NOW PLAYING" else "  PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOf(state.subtitle, state.album).filter { it.isNotEmpty() && it != state.title }.distinct()
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledIconButton(onClick = player::togglePlayPause, modifier = Modifier.size(48.dp)) {
                PlayPauseIcon(state.isPlaying, Modifier.size(28.dp))
            }
        }
    }
}
