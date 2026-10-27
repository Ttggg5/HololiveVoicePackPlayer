package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.data.moved
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** What a [TrackList] row shows. */
data class TrackRow(
    val title: String,
    val subtitle: String,
    val artwork: Any?,
    val highlighted: Boolean = false,
    /** Small picture on the artwork's corner (the topic thumbnail); the artwork is then round. */
    val badge: Any? = null,
    val playing: Boolean = false,
    /** Faded, e.g. clips already played in the queue. */
    val dimmed: Boolean = false,
    /** How often the clip was played: a ✓ on the artwork and "N plays" before the subtitle when above 0. */
    val plays: Int = 0,
) {
    val listened: Boolean get() = plays > 0
}

/**
 * A vertical list of tracks that can be reordered with the drag handle (when [onMove] is set)
 * and removed by swiping left. [onMove] is called once per drag, when the item is dropped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> TrackList(
    items: List<T>,
    key: (T) -> Any,
    row: (T) -> TrackRow,
    onClick: (item: T, index: Int) -> Unit,
    onRemove: (T) -> Unit,
    modifier: Modifier = Modifier,
    onMove: ((from: Int, to: Int) -> Unit)? = null,
    onLongClick: ((T) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    header: (@Composable () -> Unit)? = null,
    /** Background of a row; transparent lets the screen behind show through. */
    rowColor: Color = MaterialTheme.colorScheme.surface,
) {
    // Local copy so the list follows the finger while dragging; synced from [items] otherwise.
    var local by remember { mutableStateOf(items) }
    var dragging by remember { mutableStateOf(false) }
    val currentItems by rememberUpdatedState(items)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(items) {
        if (!dragging) local = items
    }

    // A header (if any) is the first list item; it never moves and nothing is dropped above it.
    val offset = if (header != null) 1 else 0
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (from.index >= offset && to.index >= offset) local = local.moved(from.index - offset, to.index - offset)
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        header?.let { h -> item(key = "header") { h() } }
        itemsIndexed(local, key = { _, item -> key(item) }) { index, item ->
            ReorderableItem(reorderState, key = key(item)) { isDragging ->
                var removed by remember(item) { mutableStateOf(false) }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled && !removed) {
                            removed = true
                            onRemove(item)
                        }
                        // Always snap back; the row disappears when the data updates.
                        value == SwipeToDismissBoxValue.Settled
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        // Only while swiping, so see-through rows don't show it at rest.
                        if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    },
                ) {
                    val data = row(item)
                    Surface(
                        tonalElevation = if (isDragging) 6.dp else 0.dp,
                        shadowElevation = if (isDragging) 6.dp else 0.dp,
                        color = when {
                            isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
                            data.highlighted && rowColor.alpha < 1f -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            data.highlighted -> MaterialTheme.colorScheme.primaryContainer
                            else -> rowColor
                        },
                        // Explicit, since a see-through row would otherwise inherit whatever is behind it.
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (data.dimmed) 0.5f else 1f)
                                .combinedClickable(
                                    onClick = { onClick(item, index) },
                                    onLongClick = onLongClick?.let { callback -> { callback(item) } },
                                )
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        ) {
                            Box {
                                if (data.badge == null) {
                                    Artwork(data.artwork, Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)), iconSize = 20.dp)
                                } else {
                                    Box(Modifier.size(48.dp)) {
                                        Artwork(data.artwork, Modifier.size(44.dp).clip(CircleShape), iconSize = 20.dp)
                                        Artwork(
                                            data.badge,
                                            Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp)),
                                            iconSize = 12.dp,
                                        )
                                    }
                                }
                                if (data.listened) ListenedBadge(18.dp, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    data.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (data.highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (data.subtitle.isNotEmpty()) {
                                    Text(
                                        listenedLine(data.plays, data.subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (data.highlighted) {
                                EqualizerBars(playing = data.playing)
                            }
                            if (onMove != null) {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragging = true
                                        },
                                        onDragStopped = {
                                            val k = key(item)
                                            val from = currentItems.indexOfFirst { key(it) == k }
                                            val to = local.indexOfFirst { key(it) == k }
                                            if (from >= 0 && to >= 0 && from != to) onMove(from, to)
                                            dragging = false
                                        },
                                    ),
                                ) {
                                    Icon(Icons.Filled.DragHandle, contentDescription = "Reorder")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
