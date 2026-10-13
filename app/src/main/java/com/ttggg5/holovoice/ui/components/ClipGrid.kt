package com.ttggg5.holovoice.ui.components

import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.ClipSort
import com.ttggg5.holovoice.data.ClipView
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.artwork
import com.ttggg5.holovoice.data.thumbnail
import com.ttggg5.holovoice.data.speaker
import com.ttggg5.holovoice.data.talentImages
import com.ttggg5.holovoice.data.talents.TalentMatcher
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState

/** Which picture a clip tile shows. */
enum class ClipArt { TALENT, TOPIC }

/**
 * A browsable clip collection: optional filter field, sort menu and grid/list toggle on top,
 * then the clips. Tap plays the shown list from that clip (or pauses the current one),
 * long-press opens the clip actions, the star toggles favorite (with undo).
 * [header] scrolls with the content (e.g. a talent's profile header).
 */
@Composable
fun ClipBrowser(
    clips: List<ClipWithPack>,
    playerState: PlayerState,
    player: PlayerController,
    onToggleFavorite: (ClipWithPack) -> Unit,
    modifier: Modifier = Modifier,
    showPack: Boolean = true,
    showCategory: Boolean = true,
    searchable: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    emptyTitle: String = "No clips here",
    emptyBody: String = "",
    artwork: ClipArt = ClipArt.TOPIC,
) {
    val prefs = LocalContext.current.appContainer.uiPrefs
    val sort by prefs.clipSort.collectAsStateWithLifecycle()
    val view by prefs.clipView.collectAsStateWithLifecycle()
    val cellDp by prefs.gridCellDp.collectAsStateWithLifecycle()
    val doubleTap by prefs.doubleTapFavorite.state.collectAsStateWithLifecycle()
    val undo = LocalUndo.current
    val haptics = LocalHapticFeedback.current
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(clips, sort, query) { sort.sorted(clips.filter { it.matches(query) }) }

    var actionsFor by remember { mutableStateOf<ClipWithPack?>(null) }
    actionsFor?.let { target ->
        // Show the latest favorite state if it changed while the sheet is open.
        val fresh = clips.firstOrNull { it.clip.id == target.clip.id } ?: target
        ClipActionsSheet(fresh, player, onToggleFavorite, onDismiss = { actionsFor = null })
    }

    val toggleFavorite: (ClipWithPack) -> Unit = { item ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onToggleFavorite(item)
        if (item.clip.isFavorite) {
            undo?.show("Removed \"${item.clip.title}\" from favorites") {
                onToggleFavorite(item.copy(clip = item.clip.copy(isFavorite = false)))
            }
        }
    }
    val playNext: (ClipWithPack) -> Unit = { item ->
        player.playNext(listOf(item))
        undo?.show("Playing next: ${item.clip.title}")
    }
    val onLongPress: (ClipWithPack) -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        actionsFor = it
    }

    val toolbar: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (searchable && clips.size > 1) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Filter ${clips.size} clips") },
                    leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear filter") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    "${clips.size} clips",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
            }
            SortMenu(ClipSort.entries, sort, { it.label }, prefs::setClipSort)
            ViewToggle(view, prefs::setClipView)
        }
    }
    val empty: @Composable () -> Unit = {
        if (query.isNotEmpty()) {
            EmptyMessage("No matches", "Nothing matches \"$query\".", icon = Icons.Filled.SearchOff, fill = false)
        } else {
            EmptyMessage(emptyTitle, emptyBody, fill = false)
        }
    }
    val play: (Int) -> Unit = { index -> player.playFrom(shown, index) }

    if (view == ClipView.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = cellDp.dp),
            modifier = modifier.fillMaxSize().pinchToResize(prefs),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            header?.let { h -> item(key = "header", span = { GridItemSpan(maxLineSpan) }) { h() } }
            item(key = "toolbar", span = { GridItemSpan(maxLineSpan) }) { toolbar() }
            if (shown.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { empty() }
            itemsIndexed(shown, key = { _, it -> it.clip.id }) { index, item ->
                ClipSwipeActions(
                    isFavorite = item.clip.isFavorite,
                    onPlayNext = { playNext(item) },
                    onToggleFavorite = { toggleFavorite(item) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                ) {
                ClipTile(
                    item = item,
                    artwork = artwork,
                    isCurrent = playerState.clipId == item.clip.id,
                    isPlaying = playerState.clipId == item.clip.id && playerState.isPlaying,
                    showPack = showPack,
                    showCategory = showCategory,
                    onClick = { play(index) },
                    onLongClick = { onLongPress(item) },
                    onToggleFavorite = { toggleFavorite(item) },
                    doubleTapEnabled = doubleTap,
                )
                }
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            header?.let { h -> item(key = "header") { Box(Modifier.padding(horizontal = 12.dp)) { h() } } }
            item(key = "toolbar") { Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { toolbar() } }
            if (shown.isEmpty()) item(key = "empty") { empty() }
            itemsIndexed(shown, key = { _, it -> it.clip.id }) { index, item ->
                ClipSwipeActions(
                    isFavorite = item.clip.isFavorite,
                    onPlayNext = { playNext(item) },
                    onToggleFavorite = { toggleFavorite(item) },
                    shape = RectangleShape,
                    modifier = Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade()),
                ) {
                ClipRow(
                    item = item,
                    artwork = artwork,
                    isCurrent = playerState.clipId == item.clip.id,
                    isPlaying = playerState.clipId == item.clip.id && playerState.isPlaying,
                    showPack = showPack,
                    showCategory = showCategory,
                    onClick = { play(index) },
                    onLongClick = { onLongPress(item) },
                    onToggleFavorite = { toggleFavorite(item) },
                    doubleTapEnabled = doubleTap,
                )
                }
            }
        }
    }
}

private fun ClipWithPack.picture(art: ClipArt): String? =
    if (art == ClipArt.TOPIC) thumbnail else artwork

/** Matches the filter text against title, talent, topic and category (any script / width). */
private fun ClipWithPack.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    val nq = TalentMatcher.normalize(query)
    return listOfNotNull(clip.title, speaker, packName, clip.category).any { field ->
        field.lowercase().contains(q) || (nq.isNotEmpty() && TalentMatcher.normalize(field).contains(nq))
    }
}

private fun ClipWithPack.metaLine(showPack: Boolean, showCategory: Boolean): String = listOfNotNull(
    speaker.takeIf { it != clip.title },
    if (showPack) packName else null,
    if (showCategory) clip.category else null,
    clip.durationMs?.let(::formatDuration),
).joinToString(" · ")

/**
 * Big picture tile: the picture on top (the topic thumbnail with a small talent badge, or the
 * talent's picture), title and details below.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipTile(
    item: ClipWithPack,
    artwork: ClipArt,
    isCurrent: Boolean,
    isPlaying: Boolean,
    showPack: Boolean,
    showCategory: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    doubleTapEnabled: Boolean = true,
) {
    val container by animateColorAsState(
        if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(350),
        label = "tileColor",
    )
    val borderColor by animateColorAsState(
        if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(350),
        label = "tileBorder",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var pop by remember { mutableStateOf(false) }
    LaunchedEffect(pop) {
        if (pop) {
            delay(650)
            pop = false
        }
    }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "tilePress")
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
                onDoubleClick = if (doubleTapEnabled) {
                    {
                        // Double-tap: favorite with a little pop.
                        if (!item.clip.isFavorite) pop = true
                        onToggleFavorite()
                    }
                } else {
                    null
                },
            ),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Artwork(item.picture(artwork), Modifier.fillMaxSize(), iconSize = 44.dp)
            androidx.compose.animation.AnimatedVisibility(
                visible = pop,
                enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(targetScale = 1.6f) + fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(84.dp),
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isCurrent,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                    EqualizerBars(isPlaying, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp))
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(40.dp),
            ) {
                FavoriteButton(item.clip.isFavorite, onToggleFavorite)
            }
            if (artwork == ClipArt.TOPIC && item.coverUri != null && item.artwork != item.coverUri) {
                // Who is speaking: the talent's picture, or a split collage for several talents.
                Artwork(
                    item.artwork,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    iconSize = 18.dp,
                )
            }
            item.clip.trackNo?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                ) {
                    Text(
                        "#$it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                item.clip.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = item.metaLine(showPack, showCategory)
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Compact one-line row for list view. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipRow(
    item: ClipWithPack,
    artwork: ClipArt,
    isCurrent: Boolean,
    isPlaying: Boolean,
    showPack: Boolean,
    showCategory: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    doubleTapEnabled: Boolean = true,
) {
    val background by animateColorAsState(
        // Opaque so the swipe-action background only shows beside the row while it moves.
        if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(350),
        label = "rowColor",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onDoubleClick = if (doubleTapEnabled) onToggleFavorite else null)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box {
            Artwork(
                item.picture(artwork),
                Modifier.size(48.dp).clip(if (artwork == ClipArt.TOPIC) RoundedCornerShape(10.dp) else CircleShape),
                iconSize = 22.dp,
            )
            if (artwork == ClipArt.TOPIC && item.coverUri != null && item.artwork != item.coverUri) {
                // Who is speaking, on the topic thumbnail's corner.
                Artwork(
                    item.artwork,
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    iconSize = 12.dp,
                )
            }
            if (isCurrent) EqualizerBars(isPlaying, size = 14.dp, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.clip.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else null,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = item.metaLine(showPack, showCategory)
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FavoriteButton(item.clip.isFavorite, onToggleFavorite)
    }
}

@Composable
private fun FavoriteButton(favorite: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (favorite) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "favScale",
    )
    IconButton(onClick = onClick) {
        Icon(
            if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (favorite) "Remove favorite" else "Add favorite",
            tint = if (favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
    }
}

fun formatDuration(ms: Long): String {
    val total = (ms + 500) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

/** Friendly message for empty screens, with an optional icon and action button. */
@Composable
fun EmptyMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    fill: Boolean = true,
) {
    Box(
        (if (fill) modifier.fillMaxSize() else modifier.fillMaxWidth()).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(44.dp))
                    }
                }
                Spacer(Modifier.size(16.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (body.isNotEmpty()) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) { Text(actionLabel) }
            }
        }
    }
}
