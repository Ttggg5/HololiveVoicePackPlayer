package com.ttggg5.holovoice.ui.settings

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.Accent
import com.ttggg5.holovoice.data.PackRepository
import com.ttggg5.holovoice.data.ThemeMode
import com.ttggg5.holovoice.data.UiPrefs
import com.ttggg5.holovoice.ui.components.LocalUndo
import com.ttggg5.holovoice.ui.nowplaying.SKIP_SECONDS
import com.ttggg5.holovoice.ui.theme.accentSwatch

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onShowHelp: () -> Unit, onOpenAlarms: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.appContainer.uiPrefs
    val undo = LocalUndo.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var confirmRemove by remember { mutableStateOf<PackRepository.SyncedFolder?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val notify: (String) -> Unit = { undo?.show(it) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                // Appearance
                Section("Appearance")
                val mode by prefs.themeMode.state.collectAsStateWithLifecycle()
                Label("Theme")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ThemeMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = mode == m,
                            onClick = { prefs.themeMode.set(m) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        ) { Text(m.label) }
                    }
                }
                val dynamic by prefs.dynamicColor.state.collectAsStateWithLifecycle()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow("Material You colours", "Use colours from your wallpaper", prefs.dynamicColor)
                }
                SwitchRow(
                    "Match the playing talent",
                    "Colour the app after the talent who is speaking; the colours below apply when nothing is playing",
                    prefs.talentColor,
                )
                val accent by prefs.accent.state.collectAsStateWithLifecycle()
                Label(if (dynamic) "Accent colour (off while Material You is on)" else "Accent colour")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Accent.entries.forEach { a ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = accentSwatch(a).copy(alpha = if (dynamic) 0.35f else 1f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !dynamic) { prefs.accent.set(a) }
                                    .then(if (accent == a) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                            ) {
                                if (accent == a) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White) }
                                }
                            }
                            Text(a.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                val cell by prefs.gridCellDp.collectAsStateWithLifecycle()
                Label("Tile size  ·  ${cell}dp (you can also pinch a grid)")
                Slider(
                    value = cell.toFloat(),
                    onValueChange = { prefs.setGridCellDp(it.toInt()) },
                    valueRange = UiPrefs.MIN_GRID_DP.toFloat()..UiPrefs.MAX_GRID_DP.toFloat(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Playback
                Section("Playback")
                ChipRow("Rewind by", SKIP_SECONDS, prefs.skipBackSec.state.collectAsStateWithLifecycle().value, { "${it}s" }) {
                    prefs.skipBackSec.set(it)
                }
                ChipRow("Skip ahead by", SKIP_SECONDS, prefs.skipForwardSec.state.collectAsStateWithLifecycle().value, { "${it}s" }) {
                    prefs.skipForwardSec.set(it)
                }
                ChipRow("Default speed for new queues", SPEEDS, prefs.defaultSpeed.state.collectAsStateWithLifecycle().value, { "${it}×" }) {
                    prefs.defaultSpeed.set(it)
                }
                SwitchRow("Pause when headphones disconnect", null, prefs.pauseOnUnplug)
                SwitchRow("Restore last queue on start", "Pick up where you left off after the app was closed", prefs.resumeOnStart)
                ListItem(
                    headlineContent = { Text("Voice alarms") },
                    supportingContent = { Text("Wake up to a talent's voice") },
                    leadingContent = { Icon(Icons.Filled.Alarm, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onOpenAlarms),
                )

                // Gestures
                Section("Gestures")
                SwitchRow("Swipe clips", "Right: play next · left: favorite", prefs.clipSwipes)
                SwitchRow("Double-tap to favorite", null, prefs.doubleTapFavorite)
                SwitchRow("Swipe between tabs and categories", null, prefs.tabSwipes)
                SwitchRow("Player swipes", "Mini player and artwork: next/previous · drag down to close · swipe up for queue", prefs.playerSwipes)
                SwitchRow("Vibration feedback", null, prefs.haptics)

                // Library & talents
                Section("Library & talents")
                SwitchRow("Sync folders when the app opens", "Pick up new and deleted files automatically", prefs.autoSyncOnStart)
                SwitchRow("Match talents on import", "Show talent pictures for new clips right away", prefs.autoMatchTalents)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    OutlinedButton(onClick = { viewModel.syncNow(notify) }, enabled = !busy) { Text("Sync now") }
                    OutlinedButton(onClick = { viewModel.refreshTalents(notify) }, enabled = !busy) { Text("Refresh talent list") }
                    OutlinedButton(onClick = { viewModel.matchTalents(notify) }, enabled = !busy) { Text("Match all talents") }
                    OutlinedButton(onClick = { viewModel.redownloadPictures(notify) }, enabled = !busy) { Text("Redownload talent pictures") }
                }
                val pictureProgress by viewModel.pictureProgress.collectAsStateWithLifecycle()
                pictureProgress?.let { (done, total) ->
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "Downloading talent pictures $done/$total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else done.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
                Label("Synced folders")
                if (folders.isEmpty()) {
                    Text(
                        "No folders imported yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                folders.forEach { f ->
                    ListItem(
                        headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(if (f.isLibrary) "Library folder · ${f.topics} topics" else "Single topic")
                        },
                        leadingContent = { Icon(if (f.isLibrary) Icons.Filled.LibraryMusic else Icons.Filled.Folder, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { confirmRemove = f }, enabled = !busy) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove ${f.name}")
                            }
                        },
                    )
                }
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Clear play history", color = MaterialTheme.colorScheme.error)
                }

                // About
                Section("About")
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
                }
                ListItem(headlineContent = { Text("Holo Voice") }, supportingContent = { Text("Version $version") })
                ListItem(
                    headlineContent = { Text("How it works") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onShowHelp),
                )
            }
        }
    }

    confirmRemove?.let { f ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove ${f.name}?") },
            text = {
                Text(
                    if (f.isLibrary) "Its ${f.topics} topics are removed from the app and the folder is no longer synced. Your files are not deleted."
                    else "The topic is removed from the app and the folder is no longer synced. Your files are not deleted.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFolder(f, notify)
                    confirmRemove = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Cancel") } },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear play history?") },
            text = { Text("Recently played and play counts are reset. Favorites and playlists stay.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory(notify)
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 12.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, pref: UiPrefs.BoolPref) {
    val on by pref.state.collectAsStateWithLifecycle()
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = on, onCheckedChange = pref::set) },
        modifier = Modifier.clickable { pref.set(!on) },
    )
}

@Composable
private fun <T> ChipRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Label(title)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}
