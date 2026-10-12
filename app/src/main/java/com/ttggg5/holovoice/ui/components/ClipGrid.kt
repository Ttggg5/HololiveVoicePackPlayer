package com.ttggg5.holovoice.ui.components

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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState

/**
 * Soundboard grid of clip tiles. Tap plays the list from that clip (or pauses the current one),
 * long-press opens the clip actions, the star toggles favorite.
 */
@Composable
fun ClipGrid(
    clips: List<ClipWithPack>,
    playerState: PlayerState,
    showPack: Boolean,
    player: PlayerController,
    onToggleFavorite: (ClipWithPack) -> Unit,
    modifier: Modifier = Modifier,
    showCategory: Boolean = false,
) {
    var actionsFor by remember { mutableStateOf<ClipWithPack?>(null) }
    actionsFor?.let { target ->
        // Show the latest favorite state if it changed while the sheet is open.
        val fresh = clips.firstOrNull { it.clip.id == target.clip.id } ?: target
        ClipActionsSheet(fresh, player, onToggleFavorite, onDismiss = { actionsFor = null })
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(clips, key = { _, it -> it.clip.id }) { index, item ->
            ClipTile(
                item = item,
                isCurrent = playerState.clipId == item.clip.id,
                isPlaying = playerState.clipId == item.clip.id && playerState.isPlaying,
                showPack = showPack,
                showCategory = showCategory,
                modifier = Modifier.animateItem(),
                onClick = { player.playFrom(clips, index) },
                onLongClick = { actionsFor = item },
                onToggleFavorite = { onToggleFavorite(item) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipTile(
    item: ClipWithPack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    showPack: Boolean,
    showCategory: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
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
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "tilePress")
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(2.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(CardDefaults.shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    // Profile picture of the talent whose voice this is.
                    item.clip.talentImageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = item.clip.talentName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                    item.clip.trackNo?.let {
                        Text(
                            text = "#$it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                AnimatedVisibility(visible = isCurrent, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    EqualizerBars(playing = isPlaying)
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (item.clip.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (item.clip.isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (item.clip.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = item.clip.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(end = 8.dp),
            )
            val meta = listOfNotNull(
                item.clip.talentName?.takeIf { it != item.clip.title },
                if (showCategory) item.clip.category else null,
                if (showPack) "${item.talent} · ${item.packName}" else null,
                item.clip.durationMs?.let(::formatDuration),
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val total = (ms + 500) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

/** Centered message used for empty lists. */
@Composable
fun EmptyMessage(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
