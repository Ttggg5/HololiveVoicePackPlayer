package com.ttggg5.holovoice.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.playback.PlayerController
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.ClipBrowser
import com.ttggg5.holovoice.ui.components.EmptyMessage

/**
 * Search: a big search bar with filter chips. Before typing it shows recent searches, your talents
 * and topic cards to browse; while typing, matching talents and topics come first, then the clips.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    player: PlayerController,
    onOpenTalent: (String) -> Unit,
    onOpenTopic: (Long) -> Unit,
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val talents by viewModel.libraryTalents.collectAsStateWithLifecycle()
    val allTalents by viewModel.talents.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val talentMatches by viewModel.talentMatches.collectAsStateWithLifecycle()
    val topicMatches by viewModel.topicMatches.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )
        TextField(
            value = filters.query,
            onValueChange = { q -> viewModel.update { it.copy(query = q) } },
            placeholder = { Text("Clips, talents, topics… (English or 日本語)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (filters.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.update { it.copy(query = "") } }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
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
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.saveSearch()
                focus.clearFocus()
            }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            FilterChip(
                selected = filters.favoritesOnly,
                onClick = { viewModel.update { it.copy(favoritesOnly = !it.favoritesOnly) } },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            val pickedTalent = talents.firstOrNull { it.slug == filters.talentSlug }
            DropdownChip(
                label = when {
                    pickedTalent != null -> pickedTalent.nameJa ?: pickedTalent.nameEn
                    filters.talentSlug == "" -> "No talent"
                    else -> "Talent"
                },
                selected = filters.talentSlug != null,
                icon = {
                    if (pickedTalent != null) {
                        Artwork(pickedTalent.imageUrl, Modifier.size(20.dp).clip(CircleShape), iconSize = 12.dp)
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
                onClear = { viewModel.update { it.copy(talentSlug = null) } },
            ) { close ->
                talents.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(listOfNotNull(t.nameJa, t.nameEn).joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Artwork(t.imageUrl, Modifier.size(28.dp).clip(CircleShape), iconSize = 16.dp) },
                        onClick = {
                            viewModel.update { it.copy(talentSlug = t.slug) }
                            close()
                        },
                    )
                }
                if (allTalents.any { it.slug == null }) {
                    DropdownMenuItem(
                        text = { Text("No talent yet") },
                        leadingIcon = { Icon(Icons.Filled.PersonOff, contentDescription = null) },
                        onClick = {
                            viewModel.update { it.copy(talentSlug = "") }
                            close()
                        },
                    )
                }
            }
            DropdownChip(
                label = topics.firstOrNull { it.pack.id == filters.packId }?.pack?.name ?: "Topic",
                selected = filters.packId != null,
                icon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClear = { viewModel.update { it.copy(packId = null) } },
            ) { close ->
                topics.sortedBy { it.pack.name.lowercase() }.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.pack.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Artwork(t.pack.coverUri, Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), iconSize = 16.dp) },
                        onClick = {
                            viewModel.update { it.copy(packId = t.pack.id) }
                            close()
                        },
                    )
                }
            }
        }
        when {
            filters.isEmpty -> Browse(
                recent = recent,
                talents = talents,
                topics = topics,
                onRecent = { q -> viewModel.update { it.copy(query = q) } },
                onRemoveRecent = viewModel::removeRecent,
                onClearRecent = viewModel::clearRecent,
                onTalent = { slug -> viewModel.update { it.copy(talentSlug = slug) } },
                onTopic = { id -> viewModel.update { it.copy(packId = id) } },
            )
            results.isEmpty() && talentMatches.isEmpty() && topicMatches.isEmpty() ->
                EmptyMessage("No matches", "Try another word or remove a filter.", icon = Icons.Filled.SearchOff)
            else -> ClipBrowser(
                clips = results,
                playerState = playerState,
                player = player,
                onToggleFavorite = viewModel::toggleFavorite,
                searchable = false,
                emptyTitle = "No clips match",
                header = if (talentMatches.isEmpty() && topicMatches.isEmpty()) {
                    null
                } else {
                    {
                        Column {
                            if (talentMatches.isNotEmpty()) {
                                SectionTitle("Talents")
                                TalentRow(talentMatches) {
                                    viewModel.saveSearch()
                                    onOpenTalent(it)
                                }
                            }
                            if (topicMatches.isNotEmpty()) {
                                SectionTitle("Topics")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(topicMatches, key = { it.pack.id }) { t ->
                                        TopicCard(t, Modifier.width(160.dp)) {
                                            viewModel.saveSearch()
                                            onOpenTopic(t.pack.id)
                                        }
                                    }
                                }
                            }
                            SectionTitle("Clips")
                        }
                    }
                },
            )
        }
    }
}

/** Browse mode: recent searches, your talents and your topics. */
@Composable
private fun Browse(
    recent: List<String>,
    talents: List<SearchViewModel.TalentHit>,
    topics: List<PackWithCount>,
    onRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onTalent: (String) -> Unit,
    onTopic: (Long) -> Unit,
) {
    if (recent.isEmpty() && talents.isEmpty() && topics.isEmpty()) {
        EmptyMessage("Search your library", "Type a name in English or 日本語, or pick a filter above.", icon = Icons.Filled.Search)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (recent.isNotEmpty()) {
            item(key = "recentTitle", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Recent searches", Modifier.weight(1f))
                    TextButton(onClick = onClearRecent) { Text("Clear") }
                }
            }
            item(key = "recent", span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    recent.forEach { q ->
                        InputChip(
                            selected = false,
                            onClick = { onRecent(q) },
                            label = { Text(q, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onRemoveRecent(q) },
                                )
                            },
                        )
                    }
                }
            }
        }
        if (talents.isNotEmpty()) {
            item(key = "talentsTitle", span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Your talents") }
            item(key = "talents", span = { GridItemSpan(maxLineSpan) }) { TalentRow(talents) { onTalent(it) } }
        }
        if (topics.isNotEmpty()) {
            item(key = "topicsTitle", span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Browse topics") }
            items(topics.sortedBy { it.pack.name.lowercase() }, key = { it.pack.id }) { t ->
                TopicCard(t, Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                    onTopic(t.pack.id)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

/** Round talent pictures with their names, side by side. */
@Composable
private fun TalentRow(talents: List<SearchViewModel.TalentHit>, onClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(talents, key = { it.slug }) { t ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick(t.slug) }.padding(vertical = 4.dp),
            ) {
                Artwork(
                    t.imageUrl,
                    Modifier.size(72.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    iconSize = 28.dp,
                )
                Text(
                    t.nameJa ?: t.nameEn,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (t.nameJa != null) {
                    Text(
                        t.nameEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A topic's thumbnail with its name and clip count on a dark gradient at the bottom. */
@Composable
private fun TopicCard(topic: PackWithCount, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Artwork(topic.pack.coverUri, Modifier.fillMaxSize(), iconSize = 36.dp, placeholder = Icons.Filled.Folder)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                topic.pack.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${topic.clipCount} clips", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

/** Filter chip that opens a menu; when [selected], tapping its trailing ✕ clears the filter. */
@Composable
private fun DropdownChip(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClear: () -> Unit,
    menu: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { open = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = icon,
            trailingIcon = {
                if (selected) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (selected) {
                DropdownMenuItem(
                    text = { Text("Any") },
                    onClick = {
                        onClear()
                        open = false
                    },
                )
            }
            menu { open = false }
        }
    }
}
