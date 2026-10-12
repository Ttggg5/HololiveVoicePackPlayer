package com.ttggg5.holovoice.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ttggg5.holovoice.appContainer
import kotlinx.coroutines.launch

/** Lets the user pick a playlist (or create one) and appends [clipIds] to it. */
@Composable
fun AddToPlaylistDialog(clipIds: List<Long>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }
    val playlists by container.repository.playlists.collectAsState(initial = emptyList())
    var newName by rememberSaveable { mutableStateOf("") }

    fun add(playlistId: Long?, name: String) {
        container.appScope.launch {
            val id = playlistId ?: container.repository.createPlaylist(name)
            container.repository.addToPlaylist(id, clipIds)
            val what = if (clipIds.size == 1) "1 clip" else "${clipIds.size} clips"
            Toast.makeText(context.applicationContext, "Added $what to $name", Toast.LENGTH_SHORT).show()
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New playlist") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { add(null, newName.trim()) }, enabled = newName.isNotBlank()) {
                        Icon(Icons.Filled.Add, contentDescription = "Create playlist")
                    }
                }
                if (playlists.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(playlists, key = { it.playlist.id }) { p ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { add(p.playlist.id, p.playlist.name) }
                                    .padding(vertical = 12.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(p.playlist.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${p.entryCount} clips",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
