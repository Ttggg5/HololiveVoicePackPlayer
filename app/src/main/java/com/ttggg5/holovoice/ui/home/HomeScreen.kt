package com.ttggg5.holovoice.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.orderedTalents
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.data.thumbnail
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.playback.PlayerState
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.BlurredCover
import com.ttggg5.holovoice.ui.components.ClipActionsSheet
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.EqualizerBars
import com.ttggg5.holovoice.ui.components.LibraryRefreshBox
import com.ttggg5.holovoice.ui.components.PlayPauseIcon
import com.ttggg5.holovoice.ui.components.TopicLine
import com.ttggg5.holovoice.ui.theme.Motion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Start page: a hero card (now playing, or the talent of the day), quick actions, then shelves. */
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
    onOpenAlarms: () -> Unit,
) {
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val recentTopics by viewModel.recentTopics.collectAsStateWithLifecycle()
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val smart by viewModel.smartLists.collectAsStateWithLifecycle()
    val state by player.state.collectAsStateWithLifecycle()
    var actionsFor by remember { mutableStateOf<ClipWithPack?>(null) }
    actionsFor?.let { ClipActionsSheet(it, player, viewModel::toggleFavorite, onDismiss = { actionsFor = null }) }

    if (topics?.isEmpty() == true) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HomeHeader(onShowHelp = onShowHelp, onOpenSettings = onOpenSettings, onOpenAlarms = onOpenAlarms)
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

    val today = smart.talentOfTheDay?.takeIf { it.clips.isNotEmpty() }
    val hasClip = state.clipId != null
    val onClipLongClick: (ClipWithPack) -> Unit = { actionsFor = it }

    // On tablets the content is centred and kept to a readable width.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LibraryRefreshBox(Modifier.widthIn(max = 840.dp).fillMaxSize().statusBarsPadding()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "header") {
            HomeHeader(onShowHelp = onShowHelp, onOpenSettings = onOpenSettings, onOpenAlarms = onOpenAlarms)
        }
        if (hasClip || today != null) {
            item(key = "hero") {
                AnimatedContent(
                    targetState = hasClip,
                    transitionSpec = {
                        (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.96f)) togetherWith fadeOut(Motion.exit())
                    },
                    label = "hero",
                ) { playing ->
                    if (playing) {
                        NowPlayingHero(state, player, onOpenNowPlaying)
                    } else if (today != null) {
                        TalentHero(today, onOpen = { onOpenTalent(today.slug) }, onShuffle = { player.playQueue(today.clips, shuffle = true) })
                    }
                }
            }
        }
        item(key = "chips") {
            QuickChips(
                onShuffleAll = { viewModel.withAllClips { player.playQueue(it, shuffle = true) } },
                onFavorites = onOpenFavorites.takeIf { favorites.isNotEmpty() },
                onNew = { player.playQueue(smart.notPlayed, shuffle = true) }.takeIf { smart.notPlayed.isNotEmpty() },
                onMostPlayed = { player.playQueue(smart.mostPlayed) }.takeIf { smart.mostPlayed.isNotEmpty() },
                onAlarms = onOpenAlarms,
            )
        }
        if (talents.isNotEmpty()) {
            item(key = "talents") {
                Section("Your talents") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(talents, key = { it.slug!! }) { t ->
                            TalentBubble(t, isPlaying = t.slug in state.talentSlugs) { onOpenTalent(t.slug!!) }
                        }
                    }
                }
            }
        }
        // While the hero shows Now playing, the talent of the day gets its own shelf.
        if (hasClip && today != null) {
            item(key = "today") {
                Section(
                    "Talent of the day",
                    action = "Shuffle",
                    onAction = { player.playQueue(today.clips, shuffle = true) },
                    subtitle = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenTalent(today.slug) },
                        ) {
                            Artwork(today.imageUrl, Modifier.size(28.dp).clip(CircleShape), iconSize = 16.dp)
                            Text(
                                "${TalentTags.clean(today.name)} · ${today.clips.size} clips",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    },
                ) {
                    ClipShelf(today.clips, state, player, onClipLongClick)
                }
            }
        }
        if (recent.isNotEmpty()) {
            item(key = "recent") {
                Section("Recently played") { ClipShelf(recent, state, player, onClipLongClick) }
            }
        }
        if (recentTopics.isNotEmpty()) {
            item(key = "topics") {
                Section("New topics", action = "All topics", onAction = onBrowseTopics) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentTopics, key = { it.pack.id }) { t ->
                            TopicCard(t, isCurrent = state.packId == t.pack.id, isPlaying = state.isPlaying) { onOpenTopic(t.pack.id) }
                        }
                    }
                }
            }
        }
        if (smart.notPlayed.isNotEmpty()) {
            item(key = "notPlayed") {
                Section("Not played yet", action = "Shuffle", onAction = { player.playQueue(smart.notPlayed, shuffle = true) }) {
                    ClipShelf(smart.notPlayed, state, player, onClipLongClick)
                }
            }
        }
        if (smart.mostPlayed.isNotEmpty()) {
            item(key = "mostPlayed") {
                Section("Most played", action = "Play all", onAction = { player.playQueue(smart.mostPlayed) }) {
                    ClipShelf(smart.mostPlayed, state, player, onClipLongClick)
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item(key = "favorites") {
                Section("Favorites", action = "See all", onAction = onOpenFavorites) {
                    ClipShelf(favorites, state, player, onClipLongClick)
                }
            }
        }
    }
    }
    }
}

@Composable
private fun HomeHeader(onShowHelp: () -> Unit, onOpenSettings: () -> Unit, onOpenAlarms: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..10 -> "Good morning"
        in 11..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val date = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                date.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onOpenAlarms) { Icon(Icons.Filled.Alarm, contentDescription = "Alarms") }
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

/** Dark wash over a blurred picture so white text on the hero stays readable. */
private val heroWash = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.72f)))

@Composable
private fun HeroFrame(background: Any?, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
    ) {
        BlurredCover(model = background, wash = heroWash, modifier = Modifier.matchParentSize(), blur = 3.dp)
        content()
    }
}

/** The playing (or paused) clip, big: tap to open Now Playing. */
@Composable
private fun NowPlayingHero(state: PlayerState, player: PlayerController, onOpen: () -> Unit) {
    val position by player.position.collectAsStateWithLifecycle(initialValue = 0L)
    HeroFrame(background = state.coverUri ?: state.talentImageUrl, onClick = onOpen) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(
                    state.artworkUri ?: state.talentImageUrl,
                    Modifier.size(84.dp).clip(CircleShape).border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                    iconSize = 36.dp,
                )
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isPlaying) EqualizerBars(true, color = Color.White, modifier = Modifier.padding(end = 6.dp))
                        Text(
                            if (state.isPlaying) "NOW PLAYING" else "CONTINUE LISTENING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.subtitle.isNotEmpty() && state.subtitle != state.title) {
                        Text(
                            state.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                FilledIconButton(
                    onClick = player::togglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.size(60.dp),
                ) {
                    PlayPauseIcon(state.isPlaying, Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            TopicLine(state.album, state.category)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** Shown when nothing is loaded: today's talent, with a shuffle button. */
@Composable
private fun TalentHero(today: HomeViewModel.TalentOfTheDay, onOpen: () -> Unit, onShuffle: () -> Unit) {
    HeroFrame(background = today.clips.firstNotNullOfOrNull { it.thumbnail } ?: today.imageUrl, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp)) {
            Artwork(
                today.imageUrl,
                Modifier.size(104.dp).clip(CircleShape).border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                iconSize = 44.dp,
            )
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Text(
                    "TALENT OF THE DAY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    TalentTags.clean(today.name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${today.clips.size} clips",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onShuffle) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Shuffle")
                }
            }
        }
    }
}

/** One-tap actions under the hero; ones with nothing to play are hidden. */
@Composable
private fun QuickChips(
    onShuffleAll: () -> Unit,
    onFavorites: (() -> Unit)?,
    onNew: (() -> Unit)?,
    onMostPlayed: (() -> Unit)?,
    onAlarms: () -> Unit,
) {
    val chips = listOfNotNull(
        Triple("Shuffle all", Icons.Filled.Shuffle, onShuffleAll),
        onFavorites?.let { Triple("Favorites", Icons.Filled.Star, it) },
        onNew?.let { Triple("New for you", Icons.Filled.AutoAwesome, it) },
        onMostPlayed?.let { Triple("Most played", Icons.Filled.LocalFireDepartment, it) },
        Triple("Alarms", Icons.Filled.Alarm, onAlarms),
    )
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(chips, key = { it.first }) { (label, icon, action) -> QuickChip(label, icon, action) }
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(20.dp),
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
private fun Section(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                subtitle?.invoke()
            }
            if (action != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(action)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun TalentBubble(t: TalentSummary, isPlaying: Boolean, onClick: () -> Unit) {
    val ring = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    ) {
        Box {
            Artwork(
                t.imageUrl,
                Modifier.size(72.dp).border(2.5.dp, ring, CircleShape).padding(4.dp).clip(CircleShape),
                iconSize = 30.dp,
            )
            if (isPlaying) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    EqualizerBars(true, size = 14.dp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Text(
            TalentTags.clean(t.name.orEmpty()),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            if (t.plays > 0) "${t.plays} plays" else "${t.clipCount} clips",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopicCard(t: PackWithCount, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Box(Modifier.width(156.dp).aspectRatio(1f).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Artwork(t.pack.coverUri, Modifier.fillMaxSize(), iconSize = 40.dp, placeholder = Icons.Filled.Folder)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
            ),
        )
        if (isCurrent) EqualizerBars(isPlaying, color = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(
                t.pack.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${t.clipCount} clips", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

/** A row of clip cards; tapping one plays [clips] from it. Shows the first 20. */
@Composable
private fun ClipShelf(
    clips: List<ClipWithPack>,
    state: PlayerState,
    player: PlayerController,
    onLongClick: (ClipWithPack) -> Unit,
) {
    val shown = clips.take(20)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(shown, key = { _, it -> it.clip.id }) { index, clip ->
            Box(Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                ClipCard(
                    clip,
                    isCurrent = state.clipId == clip.clip.id,
                    isPlaying = state.isPlaying,
                    onClick = { player.playFrom(clips, index) },
                    onLongClick = { onLongClick(clip) },
                )
            }
        }
    }
}

/** Topic thumbnail with a talent badge, title and topic below. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipCard(clip: ClipWithPack, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "cardPress")
    val talentImage = clip.orderedTalents.firstNotNullOfOrNull { it.imageUrl }
    Column(
        modifier = Modifier
            .width(132.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Box {
            val border = if (isCurrent) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)) else Modifier
            Artwork(clip.thumbnail, Modifier.size(132.dp).clip(RoundedCornerShape(18.dp)).then(border), iconSize = 40.dp)
            if (talentImage != null && talentImage != clip.thumbnail) {
                Artwork(
                    talentImage,
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    iconSize = 16.dp,
                )
            }
            if (clip.clip.isFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp),
                )
            }
            if (isCurrent) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    EqualizerBars(isPlaying, size = 16.dp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
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
