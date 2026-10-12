package com.ttggg5.holovoice.ui.search

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
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun SearchScreen(viewModel: SearchViewModel, player: PlayerController) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = { q -> viewModel.update { it.copy(query = q) } },
            placeholder = { Text("Clips, topics, talents, categories…") },
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        ) {
            FilterChip(
                selected = filters.favoritesOnly,
                onClick = { viewModel.update { it.copy(favoritesOnly = !it.favoritesOnly) } },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            DropdownChip(
                label = talents.firstOrNull { (it.slug ?: "") == filters.talentSlug }?.let { it.name ?: "No talent" } ?: "Talent",
                selected = filters.talentSlug != null,
                icon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClear = { viewModel.update { it.copy(talentSlug = null) } },
            ) { close ->
                talents.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.name ?: "No talent yet", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Artwork(t.imageUrl, Modifier.size(28.dp).clip(CircleShape), iconSize = 16.dp) },
                        onClick = {
                            viewModel.update { it.copy(talentSlug = t.slug ?: "") }
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
                        onClick = {
                            viewModel.update { it.copy(packId = t.pack.id) }
                            close()
                        },
                    )
                }
            }
        }
        when {
            filters.isEmpty -> EmptyMessage(
                "Search your library",
                "Type a name in English or 日本語, or pick a filter above.",
                icon = Icons.Filled.Search,
            )
            results.isEmpty() -> EmptyMessage("No matches", "Try another word or remove a filter.", icon = Icons.Filled.SearchOff)
            else -> ClipBrowser(
                clips = results,
                playerState = playerState,
                player = player,
                onToggleFavorite = viewModel::toggleFavorite,
                searchable = false,
            )
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
