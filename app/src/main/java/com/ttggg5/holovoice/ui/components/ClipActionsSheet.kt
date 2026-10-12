package com.ttggg5.holovoice.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.playback.PlayerController

/** Long-press menu for a clip: play next, add to queue, add to playlist, favorite. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipActionsSheet(
    clip: ClipWithPack,
    player: PlayerController,
    onToggleFavorite: (ClipWithPack) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickPlaylist by remember { mutableStateOf(false) }
    if (pickPlaylist) {
        AddToPlaylistDialog(listOf(clip.clip.id), onDismiss = onDismiss)
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Artwork(clip.coverUri, Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)), iconSize = 22.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(clip.clip.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${clip.talent} · ${clip.packName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Action(Icons.Filled.SkipNext, "Play next") {
                player.playNext(listOf(clip))
                onDismiss()
            }
            Action(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") {
                player.addToQueue(listOf(clip))
                onDismiss()
            }
            Action(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist…") { pickPlaylist = true }
            Action(
                if (clip.clip.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                if (clip.clip.isFavorite) "Remove from favorites" else "Add to favorites",
            ) {
                onToggleFavorite(clip)
                onDismiss()
            }
        }
    }
}

@Composable
private fun Action(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
