package com.ttggg5.holovoice.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.holovoice.data.artwork
import com.ttggg5.holovoice.data.speaker
import com.ttggg5.holovoice.ui.components.Artwork
import com.ttggg5.holovoice.ui.theme.HoloVoiceTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Full-screen alarm over the lock screen: who is waking you up, with big Snooze and Stop buttons. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            HoloVoiceTheme {
                val ringing by AlarmService.ringing.collectAsStateWithLifecycle()
                // Close once the alarm has been stopped or snoozed (also from the notification).
                LaunchedEffect(Unit) {
                    delay(3_000)
                    AlarmService.ringing.collect { if (it == null) finish() }
                }
                AlarmScreen(
                    ringing,
                    onSnooze = {
                        AlarmService.send(this, AlarmService.ACTION_SNOOZE)
                        finish()
                    },
                    onStop = {
                        AlarmService.send(this, AlarmService.ACTION_STOP)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun AlarmScreen(ringing: AlarmService.Ringing?, onSnooze: () -> Unit, onStop: () -> Unit) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    val clip = ringing?.clip
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                .safeDrawingPadding()
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    ringing?.alarm?.label?.takeIf { it.isNotBlank() } ?: "Alarm",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Artwork(
                        clip?.artwork ?: clip?.coverUri,
                        Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        iconSize = 96.dp,
                        placeholder = Icons.Filled.Alarm,
                    )
                    if (clip?.coverUri != null && clip.artwork != clip.coverUri) {
                        Artwork(
                            clip.coverUri,
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(3.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                            iconSize = 32.dp,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    clip?.speaker ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    clip?.clip?.title ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(64.dp)) {
                    Icon(Icons.Filled.Snooze, contentDescription = null)
                    Text("  Snooze ${ringing?.alarm?.snoozeMinutes ?: 10} min", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = onStop, modifier = Modifier.weight(1f).height(64.dp)) {
                    Text("Stop", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
