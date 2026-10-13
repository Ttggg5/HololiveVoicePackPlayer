package com.ttggg5.holovoice.ui.alarms

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.alarm.AlarmTimes
import com.ttggg5.holovoice.data.Alarm
import com.ttggg5.holovoice.data.AlarmSource
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.PackWithCount
import com.ttggg5.holovoice.data.TalentSummary
import com.ttggg5.holovoice.data.artwork
import com.ttggg5.holovoice.data.speaker
import com.ttggg5.holovoice.data.talents.TalentMatcher
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.components.EmptyMessage
import com.ttggg5.holovoice.ui.components.LocalUndo
import java.time.DayOfWeek

/** Voice alarms: list, next alarm, and an editor for time, days and what to play. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(viewModel: AlarmsViewModel, onBack: () -> Unit) {
    val alarms by viewModel.list.collectAsStateWithLifecycle()
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val talents by viewModel.talents.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val pendingClip by viewModel.pendingClip.collectAsStateWithLifecycle()
    val undo = LocalUndo.current
    var editing by remember { mutableStateOf<Alarm?>(null) }

    // "Set as alarm…" on a clip opens the editor with that clip chosen.
    LaunchedEffect(pendingClip) {
        pendingClip?.let { id ->
            editing = Alarm(hour = 7, minute = 0, source = AlarmSource.CLIP, clipId = id)
            viewModel.clearPendingClip()
        }
    }

    val sounds = remember(clips, talents, topics) { SoundNames(clips, talents, topics) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Alarm(hour = 7, minute = 0) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add alarm") },
            )
        },
    ) { padding ->
        val list = alarms ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "hints") { PermissionHints() }
            if (list.isEmpty()) {
                item(key = "empty") {
                    EmptyMessage(
                        "No alarms yet",
                        "Wake up to a talent's voice: pick a clip, or a random one from a talent, a topic or your favorites.",
                        icon = Icons.Filled.Alarm,
                        fill = false,
                    )
                }
            } else {
                item(key = "next") {
                    val next = viewModel.next(list)
                    Text(
                        next?.let { (_, at) -> "Next alarm ${AlarmTimes.untilLabel(at - System.currentTimeMillis())}" } ?: "All alarms are off",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(list, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm,
                    sounds,
                    onClick = { editing = alarm },
                    onToggle = { viewModel.setEnabled(alarm, it) },
                )
            }
        }
    }

    editing?.let { alarm ->
        AlarmEditor(
            initial = alarm,
            sounds = sounds,
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = if (alarm.id == 0L) null else {
                {
                    viewModel.delete(alarm) { restore -> undo?.show("Alarm deleted", restore) }
                    editing = null
                }
            },
            onPreview = viewModel::preview,
        )
    }
}

/** Looks up names and pictures for an alarm's sound. */
private class SoundNames(val clips: List<ClipWithPack>, val talents: List<TalentSummary>, val topics: List<PackWithCount>) {
    private val clipById = clips.associateBy { it.clip.id }
    private val talentBySlug = talents.associateBy { it.slug }
    private val topicById = topics.associateBy { it.pack.id }

    fun clip(id: Long?) = id?.let(clipById::get)
    fun talent(slug: String?) = slug?.let(talentBySlug::get)
    fun topic(id: Long?) = id?.let(topicById::get)

    /** Picture and description of what [alarm] plays. */
    fun describe(alarm: Alarm): Pair<Any?, String> = when (alarm.source) {
        AlarmSource.CLIP -> clip(alarm.clipId)?.let { it.artwork to "${it.speaker} · ${it.clip.title}" } ?: (null to "Choose a clip")
        AlarmSource.TALENT -> talent(alarm.talentSlug)?.let { it.imageUrl to "Random clip from ${it.name}" } ?: (null to "Choose a talent")
        AlarmSource.TOPIC -> topic(alarm.packId)?.let { it.pack.coverUri to "Random clip from ${it.pack.name}" } ?: (null to "Choose a topic")
        AlarmSource.FAVORITES -> null to "A random favorite"
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, sounds: SoundNames, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val dim = if (alarm.enabled) 1f else 0.5f
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatTime(alarm.hour, alarm.minute, DateFormat.is24HourFormat(context)),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
                )
                Text(
                    listOfNotNull(alarm.label.takeIf { it.isNotBlank() }, AlarmTimes.daysLabel(alarm.days)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val (picture, text) = sounds.describe(alarm)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    if (alarm.source == AlarmSource.FAVORITES) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    } else {
                        Artwork(picture, Modifier.size(24.dp).clip(CircleShape), iconSize = 14.dp)
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun formatTime(hour: Int, minute: Int, is24: Boolean): String =
    if (is24) "%02d:%02d".format(hour, minute) else "%d:%02d %s".format((hour + 11) % 12 + 1, minute, if (hour < 12) "AM" else "PM")

/** Rows explaining missing permissions that would stop alarms from ringing properly. */
@Composable
private fun PermissionHints() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val notifications = context.getSystemService(NotificationManager::class.java)
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val hints = remember(refresh) {
        buildList {
            if (!notifications.areNotificationsEnabled()) {
                add("Allow notifications so the alarm can show and be stopped" to Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                add("Allow alarms & reminders so alarms ring on time" to Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !notifications.canUseFullScreenIntent()) {
                add("Allow full-screen alarms to see the alarm on the lock screen" to Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        hints.forEach { (text, intent) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().clip(CardDefaults.shape).clickable {
                    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        "$text. Tap to open settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

private val DAYS = DayOfWeek.entries.map { it to it.name.take(1) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditor(
    initial: Alarm,
    sounds: SoundNames,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: (() -> Unit)?,
    onPreview: (Alarm) -> Unit,
) {
    val context = LocalContext.current
    val time = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
    var alarm by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<AlarmSource?>(null) }
    val current = alarm.copy(hour = time.hour, minute = time.minute)
    val ready = readyFor(alarm, sounds)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(if (initial.id == 0L) "New alarm" else "Edit alarm", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            TimePicker(state = time, modifier = Modifier.align(Alignment.CenterHorizontally))

            Text("Repeat", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                DAYS.forEach { (day, letter) ->
                    val bit = AlarmTimes.bit(day)
                    val on = alarm.days and bit != 0
                    Surface(
                        shape = CircleShape,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { alarm = alarm.copy(days = alarm.days xor bit) },
                    ) {
                        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(letter, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text(
                AlarmTimes.daysLabel(alarm.days),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            OutlinedTextField(
                value = alarm.label,
                onValueChange = { alarm = alarm.copy(label = it) },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            Text("Wake up to", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AlarmSource.entries.forEachIndexed { i, source ->
                    SegmentedButton(
                        selected = alarm.source == source,
                        onClick = {
                            alarm = alarm.copy(source = source)
                            if (source != AlarmSource.FAVORITES && !readyFor(alarm.copy(source = source), sounds)) picking = source
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, AlarmSource.entries.size),
                        icon = {},
                    ) { Text(source.label, maxLines = 1) }
                }
            }
            val (picture, text) = sounds.describe(alarm)
            ListItem(
                headlineContent = { Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(
                        when (alarm.source) {
                            AlarmSource.CLIP -> "Always this clip"
                            AlarmSource.FAVORITES -> "A different favorite each time"
                            else -> "Tap to change · a different clip each time"
                        },
                    )
                },
                leadingContent = {
                    if (alarm.source == AlarmSource.FAVORITES) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                    } else {
                        Artwork(picture, Modifier.size(40.dp).clip(CircleShape), iconSize = 20.dp)
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = alarm.source != AlarmSource.FAVORITES) { picking = alarm.source },
            )

            Text("Snooze for", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15).forEach { m ->
                    FilterChip(
                        selected = alarm.snoozeMinutes == m,
                        onClick = { alarm = alarm.copy(snoozeMinutes = m) },
                        label = { Text("$m min") },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = { onPreview(current) }, enabled = ready) { Text("Preview") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onSave(current.copy(enabled = true)) }, enabled = ready) { Text("Save") }
            }
        }
    }

    picking?.let { source ->
        SoundPicker(
            source = source,
            sounds = sounds,
            onDismiss = { picking = null },
            onPick = { picked ->
                alarm = picked(alarm)
                picking = null
            },
        )
    }
}

private fun readyFor(alarm: Alarm, sounds: SoundNames): Boolean = when (alarm.source) {
    AlarmSource.CLIP -> sounds.clip(alarm.clipId) != null
    AlarmSource.TALENT -> sounds.talent(alarm.talentSlug) != null
    AlarmSource.TOPIC -> sounds.topic(alarm.packId) != null
    AlarmSource.FAVORITES -> true
}

/** A searchable list of clips, talents or topics; [onPick] returns how to update the alarm. */
@Composable
private fun SoundPicker(source: AlarmSource, sounds: SoundNames, onDismiss: () -> Unit, onPick: ((Alarm) -> Alarm) -> Unit) {
    var query by remember { mutableStateOf("") }
    fun matches(vararg fields: String?): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        val nq = TalentMatcher.normalize(query)
        return fields.filterNotNull().any { it.lowercase().contains(q) || (nq.isNotEmpty() && TalentMatcher.normalize(it).contains(nq)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = {
            Text(
                when (source) {
                    AlarmSource.CLIP -> "Choose a clip"
                    AlarmSource.TALENT -> "Choose a talent"
                    else -> "Choose a topic"
                },
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                    when (source) {
                        AlarmSource.CLIP -> items(sounds.clips.filter { matches(it.clip.title, it.speaker, it.packName) }, key = { it.clip.id }) { c ->
                            PickRow(c.artwork, c.clip.title, "${c.speaker} · ${c.packName}") {
                                onPick { it.copy(source = AlarmSource.CLIP, clipId = c.clip.id) }
                            }
                        }
                        AlarmSource.TALENT -> items(sounds.talents.filter { matches(it.name) }, key = { it.slug ?: "" }) { t ->
                            PickRow(t.imageUrl, t.name.orEmpty(), "${t.clipCount} clips") {
                                onPick { it.copy(source = AlarmSource.TALENT, talentSlug = t.slug) }
                            }
                        }
                        else -> items(sounds.topics.filter { matches(it.pack.name, it.pack.talent) }, key = { it.pack.id }) { p ->
                            PickRow(p.pack.coverUri, p.pack.name, "${p.clipCount} clips") {
                                onPick { it.copy(source = AlarmSource.TOPIC, packId = p.pack.id) }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PickRow(picture: Any?, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Artwork(picture, Modifier.size(40.dp).clip(CircleShape), iconSize = 20.dp) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
