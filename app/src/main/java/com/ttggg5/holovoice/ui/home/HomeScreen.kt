package com.ttggg5.holovoice.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.artwork
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.ClipActionsSheet
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.PlayPauseIcon
import com.ttggg5.holovoice.ui.components.TopicLine
import java.util.Calendar

/** Start page: continue listening, recent clips, talents, new topics and favorites. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    player: PlayerController,
    onOpenTopic: (Long) -> Unit,
    onOpenTalent: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onBrowseTopics: () -> Unit,
    onShowHelp: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val recentTopics by viewModel.recentTopics.collectAsStateWithLifecycle()
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val state by player.state.collectAsStateWithLifecycle()
    var actionsFor by remember { mutableStateOf<ClipWithPack?>(null) }
    actionsFor?.let { ClipActionsSheet(it, player, viewModel::toggleFavorite, onDismiss = { actionsFor = null }) }

    if (topics?.isEmpty() == true) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HomeHeader(onShowHelp = onShowHelp, onOpenSettings = onOpenSettings, onShuffle = null)
            EmptyMessage(
                "Welcome to Holo Voice!",
                "Start by importing the folder that holds your voice packs — each sub-folder becomes a topic, " +
                    "and files named after a talent get their profile picture automatically.",
                icon = Icons.Filled.Waves,
                actionLabel = "Go to Topics to import",
                onAction = onBrowseTopics,
            )
        }
        return
    }

    // On tablets the content is centred and kept to a readable width.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            HomeHeader(onShowHelp = onShowHelp, onOpenSettings = onOpenSettings, onShuffle = { viewModel.withAllClips { player.playQueue(it, shuffle = true) } })
        }
        item(key = "continue") {
            AnimatedVisibility(visible = state.clipId != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ContinueCard(state, player, onOpenNowPlaying)
            }
        }
        if (recent.isNotEmpty()) {
            item(key = "recent") {
                Section("Recently played") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(recent, key = { _, it -> it.clip.id }) { index, clip ->
                            ClipCard(
                                clip,
                                isCurrent = state.clipId == clip.clip.id,
                                isPlaying = state.isPlaying,
                                onClick = { player.playFrom(recent, index) },
                                onLongClick = { actionsFor = clip },
                            )
                        }
                    }
                }
            }
        }
        if (talents.isNotEmpty()) {
            item(key = "talents") {
                Section("Your talents") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(talents, key = { it.slug!! }) { t ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable { onOpenTalent(t.slug!!) },
                            ) {
                                Artwork(
                                    t.imageUrl,
                                    Modifier.size(76.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    iconSize = 32.dp,
                                )
                                Text(
                                    t.name.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (recentTopics.isNotEmpty()) {
            item(key = "topics") {
                Section("Recently added topics", action = "All topics", onAction = onBrowseTopics) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentTopics, key = { it.pack.id }) { t ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenTopic(t.pack.id) },
                            ) {
                                Box {
                                    Artwork(t.pack.coverUri, Modifier.fillMaxWidth().aspectRatio(1f), iconSize = 40.dp, placeholder = Icons.Filled.Folder)
                                    if (state.packId == t.pack.id) {
                                        EqualizerBars(state.isPlaying, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                                    }
                                }
                                Column(Modifier.padding(10.dp)) {
                                    Text(t.pack.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${t.clipCount} clips",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item(key = "favorites") {
                Section("Favorites", action = "See all", onAction = onOpenFavorites) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(favorites, key = { _, it -> it.clip.id }) { index, clip ->
                            ClipCard(
                                clip,
                                isCurrent = state.clipId == clip.clip.id,
                                isPlaying = state.isPlaying,
                                onClick = { player.playFrom(favorites, index) },
                                onLongClick = { actionsFor = clip },
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun HomeHeader(onShowHelp: () -> Unit, onOpenSettings: () -> Unit, onShuffle: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..10 -> "Good morning"
        in 11..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("What do you want to hear?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onShuffle != null) {
            FilledTonalButton(onClick = onShuffle) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Shuffle all")
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        menu = false
                        onOpenSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text("How it works") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                    onClick = {
                        menu = false
                        onShowHelp()
                    },
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(state: com.ttggg5.holovoice.playback.PlayerState, player: PlayerController, onOpen: () -> Unit) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpen),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Artwork(
                state.artworkUri ?: state.talentImageUrl,
                Modifier.size(72.dp).clip(CircleShape).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                iconSize = 32.dp,
            )
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    if (state.isPlaying) "Now playing" else "Continue listening",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(state.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.subtitle.isNotEmpty() && state.subtitle != state.title) {
                    Text(state.subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TopicLine(state.album, state.category)
            }
            FilledIconButton(onClick = player::togglePlayPause, modifier = Modifier.size(56.dp)) {
                PlayPauseIcon(state.isPlaying, Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, action: String? = null, onAction: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipCard(clip: ClipWithPack, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }),
    ) {
        Box {
            Artwork(clip.artwork, Modifier.size(128.dp).clip(RoundedCornerShape(16.dp)), iconSize = 40.dp)
            if (clip.clip.isFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp),
                )
            }
            if (isCurrent) EqualizerBars(isPlaying, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Text(
            clip.clip.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp),
        )
        Text(
            clip.packName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
