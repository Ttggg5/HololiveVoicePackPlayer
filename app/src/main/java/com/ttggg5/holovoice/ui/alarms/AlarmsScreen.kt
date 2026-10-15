package com.ttggg5.holovoice.ui.alarms

import com.ttggg5.holovoice.alarm.AlarmLog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import android.os.PowerManager
import android.media.AudioManager
import android.annotation.SuppressLint
import com.ttggg5.holovoice.ui.theme.Motion
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.ttggg5.holovoice.data.talents.TalentTags
import com.ttggg5.holovoice.ui.components.BlurredCover
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

/** Voice alarms: a next-alarm hero, the alarm cards, and an editor for time, days and what to play. */
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
    var menu by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    if (showLog) AlarmLogDialog(onDismiss = { showLog = false })

    // "Set as alarm…" on a clip opens the editor with that clip chosen.
    LaunchedEffect(pendingClip) {
        pendingClip?.let { id ->
            editing = Alarm(hour = 7, minute = 0, source = AlarmSource.CLIP, clipId = id)
            viewModel.clearPendingClip()
        }
    }

    val sounds = remember(clips, talents, topics) { SoundNames(clips, talents, topics) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Alarms", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Alarm log") },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    showLog = true
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    NextAlarmHero(viewModel.next(list), sounds) { editing = it }
                }
                item(key = "title") {
                    Text(
                        "${list.size} alarms · ${list.count { it.enabled }} on",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }
            items(list, key = { it.id }) { alarm ->
                Box(Modifier.animateItem(fadeInSpec = Motion.itemFade(), placementSpec = Motion.itemPlacement, fadeOutSpec = Motion.itemFade())) {
                    AlarmCard(
                        alarm,
                        sounds,
                        onClick = { editing = alarm },
                        onToggle = { viewModel.setEnabled(alarm, it) },
                    )
                }
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
        AlarmSource.TALENT -> talent(alarm.talentSlug)?.let { it.imageUrl to "Random clip from ${TalentTags.clean(it.name.orEmpty())}" } ?: (null to "Choose a talent")
        AlarmSource.TOPIC -> topic(alarm.packId)?.let { it.pack.coverUri to "Random clip from ${it.pack.name}" } ?: (null to "Choose a topic")
        AlarmSource.FAVORITES -> null to "A random favorite"
    }

    /** A picture to blur behind the alarm: the clip's topic thumbnail, the talent or the topic. */
    fun backdrop(alarm: Alarm): Any? = when (alarm.source) {
        AlarmSource.CLIP -> clip(alarm.clipId)?.let { it.coverUri ?: it.artwork }
        AlarmSource.TALENT -> talent(alarm.talentSlug)?.imageUrl
        AlarmSource.TOPIC -> topic(alarm.packId)?.pack?.coverUri
        AlarmSource.FAVORITES -> null
    }
}

/** The next alarm, big, over its sound's picture; or a note that every alarm is off. */
@Composable
private fun NextAlarmHero(next: Pair<Alarm, Long>?, sounds: SoundNames, onOpen: (Alarm) -> Unit) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(primary, tertiary)))
            .then(if (next != null) Modifier.clickable { onOpen(next.first) } else Modifier),
    ) {
        if (next != null) {
            BlurredCover(
                model = sounds.backdrop(next.first),
                wash = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.6f))),
                modifier = Modifier.matchParentSize(),
                blur = 6.dp,
            )
        }
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    if (next != null) "  NEXT ALARM" else "  ALL ALARMS ARE OFF",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            if (next == null) {
                Text(
                    "Turn one on below, or add a new one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp),
                )
                return@Column
            }
            val (alarm, at) = next
            TimeText(alarm.hour, alarm.minute, DateFormat.is24HourFormat(context), color = Color.White, size = 56)
            Text(
                listOf(AlarmTimes.untilLabel(at - System.currentTimeMillis()).replaceFirstChar { it.uppercase() }, alarm.label)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            SoundChip(alarm, sounds, onDark = true)
        }
    }
}

/** Hour and minute in a large light face, with a smaller AM/PM. */
@Composable
private fun TimeText(hour: Int, minute: Int, is24: Boolean, color: Color, size: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            if (is24) "%02d:%02d".format(hour, minute) else "%d:%02d".format((hour + 11) % 12 + 1, minute),
            fontSize = size.sp,
            fontWeight = FontWeight.Light,
            color = color,
        )
        if (!is24) {
            Text(
                if (hour < 12) "AM" else "PM",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                modifier = Modifier.padding(start = 6.dp, bottom = (size / 6).dp),
            )
        }
    }
}

/** A pill with the picture and description of what the alarm plays. */
@Composable
private fun SoundChip(alarm: Alarm, sounds: SoundNames, onDark: Boolean = false) {
    val (picture, text) = sounds.describe(alarm)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (onDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        if (alarm.source == AlarmSource.FAVORITES) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
            }
        } else {
            Artwork(picture, Modifier.size(26.dp).clip(CircleShape), iconSize = 14.dp)
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, sounds: SoundNames, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val content by animateFloatAsState(if (alarm.enabled) 1f else 0.5f, tween(Motion.MEDIUM), label = "alarmDim")
    val container by animateColorAsState(
        if (alarm.enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(Motion.MEDIUM),
        label = "alarmCard",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = container,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).alpha(content)) {
                    if (alarm.label.isNotBlank()) {
                        Text(
                            alarm.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TimeText(alarm.hour, alarm.minute, DateFormat.is24HourFormat(context), color = MaterialTheme.colorScheme.onSurface, size = 44)
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(content),
            ) {
                if (alarm.days == 0) {
                    Text("Once", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    DAYS.forEach { (day, letter) ->
                        val on = alarm.days and AlarmTimes.bit(day) != 0
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                letter,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "  " + AlarmTimes.daysLabel(alarm.days),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.padding(top = 10.dp).alpha(content)) { SoundChip(alarm, sounds) }
        }
    }
}

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
    val audio = context.getSystemService(AudioManager::class.java)
    val power = context.getSystemService(PowerManager::class.java)
    val hints = remember(refresh) {
        buildList {
            if (!notifications.areNotificationsEnabled()) {
                add("Allow notifications so the alarm can show and be stopped" to Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                add("Allow alarms & reminders so alarms ring on time" to Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                add("Your alarm volume is off, so alarms are silent" to Intent(Settings.ACTION_SOUND_SETTINGS))
            }
            if (!power.isIgnoringBatteryOptimizations(context.packageName)) {
                @SuppressLint("BatteryLife")
                val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                add("Let Holo Voice run in the background so the voice can play when the alarm rings" to request)
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
                trailingContent = if (alarm.source != AlarmSource.FAVORITES) {
                    { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                } else {
                    null
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                TextButton(onClick = { onPreview(current) }, enabled = ready) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Preview")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(current.copy(enabled = true)) }, enabled = ready) { Text("Save") }
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

/** What happened when alarms went off, to explain (and report) an alarm that didn't ring. */
@Composable
private fun AlarmLogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var log by remember { mutableStateOf(AlarmLog.read(context)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alarm log") },
        text = {
            Text(
                log.ifBlank { "Nothing yet. The next time an alarm goes off, what happened is listed here." },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Holo Voice alarm log", log))
                Toast.makeText(context, "Alarm log copied", Toast.LENGTH_SHORT).show()
            }, enabled = log.isNotBlank()) { Text("Copy") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    AlarmLog.clear(context)
                    log = ""
                }, enabled = log.isNotBlank()) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}
