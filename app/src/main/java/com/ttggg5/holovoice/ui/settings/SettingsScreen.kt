package com.ttggg5.holovoice.ui.settings

import com.ttggg5.holovoice.ui.components.CrashReportDialog
import com.ttggg5.holovoice.CrashLog
import androidx.compose.material.icons.filled.BugReport
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ttggg5.holovoice.data.backup.BackupSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.ttggg5.holovoice.ui.talents.StatTile
import com.ttggg5.holovoice.ui.theme.accentSwatch

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * Settings: a library summary on top, then grouped cards (Appearance, Playback, Gestures, Library &
 * talents, Data, About) whose rows each have a coloured icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onShowHelp: () -> Unit, onOpenAlarms: () -> Unit, onOpenStats: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.appContainer.uiPrefs
    val undo = LocalUndo.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var confirmRemove by remember { mutableStateOf<PackRepository.SyncedFolder?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val notify: (String) -> Unit = { undo?.show(it) }
    var crashReport by remember { mutableStateOf(CrashLog.read(context)) }
    var showCrash by remember { mutableStateOf(false) }
    if (showCrash) {
        crashReport?.let { report ->
            CrashReportDialog(report, title = "Last crash report", onDismiss = {
                CrashLog.clear(context)
                crashReport = null
                showCrash = false
            })
        }
    }
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val restoreResult by viewModel.restoreResult.collectAsStateWithLifecycle()
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.backUp(it, notify) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.openBackup(it, notify) }
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            ) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)))

                LibrarySummary(stats, version)

                // Appearance
                val mode by prefs.themeMode.state.collectAsStateWithLifecycle()
                val dynamic by prefs.dynamicColor.state.collectAsStateWithLifecycle()
                val accent by prefs.accent.state.collectAsStateWithLifecycle()
                val cell by prefs.gridCellDp.collectAsStateWithLifecycle()
                Group("Appearance") {
                    SettingBlock(Icons.Filled.DarkMode, IconTint.Indigo, "Theme") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { i, m ->
                                SegmentedButton(
                                    selected = mode == m,
                                    onClick = { prefs.themeMode.set(m) },
                                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                                ) { Text(m.label) }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Divider()
                        SwitchRow(Icons.Filled.Wallpaper, IconTint.Teal, "Material You colours", "Use colours from your wallpaper", prefs.dynamicColor)
                    }
                    Divider()
                    SwitchRow(
                        Icons.Filled.Face,
                        IconTint.Pink,
                        "Match the playing talent",
                        "Colour the app after the talent who is speaking; the accent below applies when nothing is playing",
                        prefs.talentColor,
                    )
                    Divider()
                    SettingBlock(
                        Icons.Filled.Palette,
                        IconTint.Orange,
                        "Accent colour",
                        if (dynamic) "Off while Material You is on" else null,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Accent.entries.forEach { a -> AccentSwatch(a, selected = accent == a, enabled = !dynamic) { prefs.accent.set(a) } }
                        }
                    }
                    Divider()
                    SettingBlock(Icons.Filled.GridView, IconTint.Blue, "Tile size", "${cell}dp · you can also pinch a grid") {
                        Slider(
                            value = cell.toFloat(),
                            onValueChange = { prefs.setGridCellDp(it.toInt()) },
                            valueRange = UiPrefs.MIN_GRID_DP.toFloat()..UiPrefs.MAX_GRID_DP.toFloat(),
                        )
                    }
                }

                // Playback
                Group("Playback") {
                    SettingBlock(Icons.Filled.FastRewind, IconTint.Blue, "Rewind by") {
                        Chips(SKIP_SECONDS, prefs.skipBackSec.state.collectAsStateWithLifecycle().value, { "${it}s" }, prefs.skipBackSec::set)
                    }
                    Divider()
                    SettingBlock(Icons.Filled.FastForward, IconTint.Blue, "Skip ahead by") {
                        Chips(SKIP_SECONDS, prefs.skipForwardSec.state.collectAsStateWithLifecycle().value, { "${it}s" }, prefs.skipForwardSec::set)
                    }
                    Divider()
                    SettingBlock(Icons.Filled.Speed, IconTint.Orange, "Default speed", "For new queues") {
                        Chips(SPEEDS, prefs.defaultSpeed.state.collectAsStateWithLifecycle().value, { "${it}×" }, prefs.defaultSpeed::set)
                    }
                    Divider()
                    SwitchRow(Icons.Filled.Headphones, IconTint.Teal, "Pause when headphones disconnect", null, prefs.pauseOnUnplug)
                    Divider()
                    SwitchRow(Icons.Filled.OpenInFull, IconTint.Blue, "Open player when you tap a clip", "Turn off to keep browsing while it plays", prefs.openPlayerOnTap)
                    Divider()
                    SwitchRow(Icons.Filled.Restore, IconTint.Green, "Restore last queue on start", "Pick up where you left off after the app was closed", prefs.resumeOnStart)
                    Divider()
                    LinkRow(Icons.Filled.Alarm, IconTint.Pink, "Voice alarms", "Wake up to a talent's voice", onOpenAlarms)
                    Divider()
                    LinkRow(Icons.Filled.Insights, IconTint.Indigo, "Listening stats", "Time listened, top talents and clips", onOpenStats)
                }

                // Gestures
                Group("Gestures") {
                    SwitchRow(Icons.Filled.SwapHoriz, IconTint.Indigo, "Swipe clips", "Right: play next · left: favorite", prefs.clipSwipes)
                    Divider()
                    SwitchRow(Icons.Filled.Star, IconTint.Orange, "Double-tap to favorite", null, prefs.doubleTapFavorite)
                    Divider()
                    SwitchRow(Icons.Filled.TouchApp, IconTint.Blue, "Swipe between tabs", null, prefs.tabSwipes)
                    Divider()
                    SwitchRow(
                        Icons.Filled.Gesture,
                        IconTint.Teal,
                        "Player swipes",
                        "Mini player and artwork: next/previous · drag down to close · swipe up for queue",
                        prefs.playerSwipes,
                    )
                    Divider()
                    SwitchRow(Icons.Filled.Vibration, IconTint.Green, "Vibration feedback", null, prefs.haptics)
                }

                // Library & talents
                val pictureProgress by viewModel.pictureProgress.collectAsStateWithLifecycle()
                Group("Library & talents") {
                    SwitchRow(Icons.Filled.Sync, IconTint.Green, "Sync folders when the app opens", "Pick up new and deleted files automatically", prefs.autoSyncOnStart)
                    Divider()
                    SwitchRow(Icons.Filled.AutoFixHigh, IconTint.Pink, "Match talents on import", "Show talent pictures for new clips right away", prefs.autoMatchTalents)
                    Divider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile(Icons.Filled.Sync, "Sync now", !busy, Modifier.weight(1f)) { viewModel.syncNow(notify) }
                            ActionTile(Icons.Filled.CloudSync, "Refresh talent list", !busy, Modifier.weight(1f)) { viewModel.refreshTalents(notify) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionTile(Icons.Filled.Face, "Match all talents", !busy, Modifier.weight(1f)) { viewModel.matchTalents(notify) }
                            ActionTile(Icons.Filled.Download, "Redownload pictures", !busy, Modifier.weight(1f)) { viewModel.redownloadPictures(notify) }
                        }
                        pictureProgress?.let { (done, total) ->
                            Text(
                                "Downloading talent pictures $done/$total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            LinearProgressIndicator(
                                progress = { if (total == 0) 0f else done.toFloat() / total },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                }

                // Synced folders
                Group("Synced folders") {
                    if (folders.isEmpty()) {
                        Text(
                            "No folders imported yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    folders.forEachIndexed { i, f ->
                        if (i > 0) Divider()
                        SettingRow(
                            icon = if (f.isLibrary) Icons.Filled.LibraryMusic else Icons.Filled.Folder,
                            tint = if (f.isLibrary) IconTint.Indigo else IconTint.Blue,
                            title = f.name,
                            subtitle = if (f.isLibrary) "Library folder · ${f.topics} topics" else "Single topic",
                        ) {
                            IconButton(onClick = { confirmRemove = f }, enabled = !busy) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove ${f.name}")
                            }
                        }
                    }
                }

                // Backup & restore
                Group("Backup & restore") {
                    SettingRow(
                        icon = Icons.Filled.Backup,
                        tint = IconTint.Teal,
                        title = "Back up now",
                        subtitle = "Save favorites, playlists, alarms, history, custom talents and settings to a file",
                        onClick = { if (!busy) backupLauncher.launch("holovoice-backup-${LocalDate.now()}.json") },
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Filled.SettingsBackupRestore,
                        tint = IconTint.Blue,
                        title = "Restore from file…",
                        subtitle = "Import your folders first, so the clips in the backup can be found",
                        onClick = { if (!busy) restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    )
                }

                // Data
                Group("Data") {
                    SettingRow(
                        icon = Icons.Filled.DeleteSweep,
                        tint = IconTint.Red,
                        title = "Clear play history",
                        subtitle = "Reset recently played and play counts",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { confirmClear = true },
                    )
                }

                // About
                Group("About") {
                    SettingRow(Icons.Filled.Info, IconTint.Blue, "Holo Voice", "Version $version")
                    Divider()
                    LinkRow(Icons.AutoMirrored.Filled.HelpOutline, IconTint.Green, "How it works", "A quick tour of the app", onShowHelp)
                    if (crashReport != null) {
                        Divider()
                        LinkRow(Icons.Filled.BugReport, IconTint.Red, "Last crash report", "Copy the details to report the problem") { showCrash = true }
                    }
                }
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
    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            icon = { Icon(Icons.Filled.SettingsBackupRestore, contentDescription = null) },
            title = { Text("Restore this backup?") },
            text = {
                val date = remember(backup.createdAt) {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .format(Instant.ofEpochMilli(backup.createdAt).atZone(ZoneId.systemDefault()))
                }
                Text(
                    "Made $date with ${BackupSummary.of(backup).describe()}.\n\n" +
                        "Your playlists, alarms and settings are replaced by the ones in the backup. " +
                        "Favorites, play counts and history are added to what you have.",
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.confirmRestore(notify) }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancel") } },
        )
    }
    restoreResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreResult,
            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
            title = { Text("Restore finished") },
            text = {
                Text(
                    "Restored ${result.describe()}." + if (result.missingClips > 0) {
                        "\n\n${result.missingClips} clips from the backup aren't in your library. " +
                            "Import their folders, then restore again to bring back their favorites, history and playlist entries."
                    } else "",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::dismissRestoreResult) { Text("Done") } },
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

/** Colours for the small icon badges in front of each row. */
private enum class IconTint(val color: Color) {
    Blue(Color(0xFF3B82F6)),
    Indigo(Color(0xFF6366F1)),
    Teal(Color(0xFF14B8A6)),
    Green(Color(0xFF22C55E)),
    Orange(Color(0xFFF59E0B)),
    Pink(Color(0xFFEC4899)),
    Red(Color(0xFFEF4444)),
}

/** App name, version and what's in the library. */
@Composable
private fun LibrarySummary(stats: SettingsViewModel.LibraryStats, version: String) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(primary, tertiary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Waves, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Holo Voice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Version $version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                StatTile("${stats.topics}", "topics", Modifier.weight(1f))
                StatTile("${stats.clips}", "clips", Modifier.weight(1f))
                StatTile("${stats.talents}", "talents", Modifier.weight(1f))
            }
        }
    }
}

/** A titled, rounded card holding a group of rows. */
@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(content = content)
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun IconBadge(icon: ImageVector, tint: IconTint) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tint.color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint.color, modifier = Modifier.size(20.dp))
    }
}

/** Icon, title and subtitle, with an optional control on the right. */
@Composable
private fun SettingRow(
    icon: ImageVector,
    tint: IconTint,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        IconBadge(icon, tint)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** A row with a control underneath (segmented buttons, chips, a slider…). */
@Composable
private fun SettingBlock(icon: ImageVector, tint: IconTint, title: String, subtitle: String? = null, control: @Composable () -> Unit) {
    Column {
        SettingRow(icon, tint, title, subtitle)
        Box(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 12.dp)) { control() }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, tint: IconTint, title: String, subtitle: String?, pref: UiPrefs.BoolPref) {
    val on by pref.state.collectAsStateWithLifecycle()
    SettingRow(icon, tint, title, subtitle, onClick = { pref.set(!on) }) {
        Switch(checked = on, onCheckedChange = pref::set)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, tint: IconTint, title: String, subtitle: String?, onClick: () -> Unit) {
    SettingRow(icon, tint, title, subtitle, onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}

@Composable
private fun AccentSwatch(accent: Accent, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentSwatch(accent).copy(alpha = if (enabled) 1f else 0.35f))
                .clickable(enabled = enabled, onClick = onClick)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
        Text(accent.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A tonal button tile for one-off library actions. */
@Composable
private fun ActionTile(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
