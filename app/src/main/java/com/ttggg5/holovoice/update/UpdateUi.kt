package com.ttggg5.holovoice.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import kotlinx.coroutines.launch

/** The release the Home card is about, or null when there's nothing to show there. */
fun UpdateState.cardRelease(dismissed: Int): Release? = when (this) {
    is UpdateState.Available -> release.takeIf { it.versionCode != dismissed }
    is UpdateState.NeedsPermission -> release
    is UpdateState.Downloading -> release
    is UpdateState.Installing -> release
    is UpdateState.Failed -> release
    else -> null
}

/** Starts (or continues after the permission step) installing [release]; survives leaving the screen. */
fun startUpdate(context: android.content.Context, release: Release) {
    val container = context.appContainer
    val updater = container.updater
    if (!updater.canInstall()) {
        context.startActivity(updater.installPermissionIntent())
    }
    container.appScope.launch { updater.downloadAndInstall(release) }
}

/** Home card: "Update available", with Later / Install, the download progress, or what went wrong. */
@Composable
fun UpdateCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val updater = context.appContainer.updater
    val state by updater.state.collectAsStateWithLifecycle()
    val dismissed by updater.dismissed.collectAsStateWithLifecycle()
    val release = state.cardRelease(dismissed) ?: return
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        "Update available · ${release.versionName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        subtitle(state, updater.installedVersionName, release),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when (val s = state) {
                is UpdateState.Downloading -> LinearProgressIndicator(
                    progress = { s.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                is UpdateState.Installing -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                else -> Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    TextButton(onClick = { updater.dismiss(release) }) { Text("Later") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { startUpdate(context, release) }) {
                        Text(if (s is UpdateState.Failed) "Try again" else "Install")
                    }
                }
            }
        }
    }
}

private fun subtitle(state: UpdateState, installed: String, release: Release): String = when (state) {
    is UpdateState.Downloading -> "Downloading… ${(state.progress * 100).toInt()}%"
    is UpdateState.Installing -> "Confirm the update when Android asks"
    is UpdateState.NeedsPermission -> "Allow Holo Voice to install apps, then tap Install again"
    is UpdateState.Failed -> state.message
    else -> listOfNotNull("You have $installed".takeIf { installed.isNotEmpty() }, release.notes.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
}

/** Settings: details of an available update, with Install. */
@Composable
fun UpdateDialog(release: Release, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val updater = context.appContainer.updater
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
        title = { Text("Update to ${release.versionName}?") },
        text = {
            Column {
                Text("You have ${updater.installedVersionName}." + if (release.sizeBytes > 0) " The download is ${release.sizeBytes / (1024 * 1024)} MB." else "")
                if (release.notes.isNotBlank()) {
                    Text(
                        release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (!updater.canInstall()) {
                    Text(
                        "Android will first ask you to allow Holo Voice to install apps. Allow it, come back and tap Install again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                startUpdate(context, release)
                onDismiss()
            }) { Text("Install") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
