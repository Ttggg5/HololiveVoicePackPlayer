package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
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
    val playing: Boolean = false,
)

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
) {
    // Local copy so the list follows the finger while dragging; synced from [items] otherwise.
    var local by remember { mutableStateOf(items) }
    var dragging by remember { mutableStateOf(false) }
    val currentItems by rememberUpdatedState(items)
    LaunchedEffect(items) {
        if (!dragging) local = items
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        local = local.moved(from.index, to.index)
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
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
                        Box(
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
                        color = if (data.highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onClick(item, index) },
                                    onLongClick = onLongClick?.let { callback -> { callback(item) } },
                                )
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        ) {
                            Artwork(data.artwork, Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)), iconSize = 20.dp)
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
                                        data.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (data.playing) {
                                Icon(
                                    Icons.Filled.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            if (onMove != null) {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { dragging = true },
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
