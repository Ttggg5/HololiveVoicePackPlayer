package com.ttggg5.holovoice.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ttggg5.holovoice.R
import com.ttggg5.holovoice.appContainer
import com.ttggg5.holovoice.data.Alarm
import com.ttggg5.holovoice.data.ClipWithPack
import com.ttggg5.holovoice.data.artwork
import com.ttggg5.holovoice.data.speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Rings an alarm: plays its clip through the alarm volume on repeat (volume rising over the first
 * seconds), shows a full-screen alarm over the lock screen, and handles Snooze / Stop. Gives up
 * after [TIMEOUT_MS] and counts that as a snooze.
 */
@OptIn(UnstableApi::class)
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringingId: Long = -1
    private var rampStep = 0

    private val ramp = object : Runnable {
        override fun run() {
            val p = player ?: return
            rampStep++
            p.volume = (START_VOLUME + (1f - START_VOLUME) * rampStep / RAMP_STEPS).coerceAtMost(1f)
            if (rampStep < RAMP_STEPS) handler.postDelayed(this, RAMP_MS / RAMP_STEPS)
        }
    }

    private val timeout = Runnable { snoozeAndStop() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                val id = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
                // Go to the foreground right away; the clip and picture are filled in once loaded.
                startInForeground(notification(null, null, null))
                if (id >= 0) ring(id) else stopRinging()
            }
            ACTION_SNOOZE -> snoozeAndStop()
            ACTION_STOP -> stopRinging()
            else -> if (ringingId < 0) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
    }

    private fun ring(alarmId: Long) {
        // A second alarm while one is ringing replaces it.
        releasePlayer()
        ringingId = alarmId
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HoloVoice:alarm")
            .apply { acquire(TIMEOUT_MS + 60_000L) }
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
        scope.launch {
            val repo = appContainer.alarms
            val alarm = repo.get(alarmId)
            if (alarm == null) {
                stopRinging()
                return@launch
            }
            repo.onRang(alarm)
            val clip = repo.pickClip(alarm)
            val picture = withContext(Dispatchers.IO) { clip?.let(::loadPicture) }
            _ringing.value = Ringing(alarm, clip)
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(alarm, clip, picture))
            play(clip?.clip?.fileUri?.let(Uri::parse))
        }
    }

    private fun play(uri: Uri?) {
        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                /* handleAudioFocus = */ true,
            )
            .build()
        player = p
        p.repeatMode = Player.REPEAT_MODE_ONE
        p.volume = START_VOLUME
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // The clip couldn't be read (moved, no access): ring with the system alarm sound instead.
                val fallback = RingtoneManager.getActualDefaultRingtoneUri(this@AlarmService, RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (fallback != null && p.currentMediaItem?.localConfiguration?.uri != fallback) {
                    p.setMediaItem(MediaItem.fromUri(fallback))
                    p.prepare()
                    p.play()
                }
            }
        })
        val source = uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (source != null) {
            p.setMediaItem(MediaItem.fromUri(source))
            p.prepare()
            p.play()
        }
        rampStep = 0
        handler.removeCallbacks(ramp)
        handler.postDelayed(ramp, RAMP_MS / RAMP_STEPS)
    }

    private fun snoozeAndStop() {
        val id = ringingId
        if (id >= 0) appContainer.appScope.launch { appContainer.alarms.snooze(id) }
        stopRinging()
    }

    private fun stopRinging() {
        ringingId = -1
        _ringing.value = null
        handler.removeCallbacks(timeout)
        releasePlayer()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        handler.removeCallbacks(ramp)
        player?.release()
        player = null
    }

    override fun onDestroy() {
        _ringing.value = null
        releasePlayer()
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }

    /** The stored talent picture (or topic thumbnail) for the notification. */
    private fun loadPicture(clip: ClipWithPack): Bitmap? = runCatching {
        val uri = Uri.parse(clip.artwork?.takeUnless { it.startsWith("holovoice-collage") } ?: clip.coverUri ?: return null)
        when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(File(uri.path!!).absolutePath)
            "content" -> contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            else -> null
        }
    }.getOrNull()

    private fun notification(alarm: Alarm?, clip: ClipWithPack?, picture: Bitmap?): Notification {
        ensureChannel(this)
        val fullScreen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = alarm?.label?.takeIf { it.isNotBlank() } ?: "Alarm"
        val text = clip?.let { "${it.speaker} · ${it.clip.title}" } ?: "Wake up!"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_holovoice)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(picture)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, "Snooze", serviceIntent(this, ACTION_SNOOZE))
            .addAction(0, "Stop", serviceIntent(this, ACTION_STOP))
            .build()
    }

    /** What is ringing, for [AlarmActivity]. */
    data class Ringing(val alarm: Alarm, val clip: ClipWithPack?)

    companion object {
        const val ACTION_RING = "com.ttggg5.holovoice.alarm.RING"
        const val ACTION_SNOOZE = "com.ttggg5.holovoice.alarm.SNOOZE"
        const val ACTION_STOP = "com.ttggg5.holovoice.alarm.STOP"
        private const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 7301
        private const val TIMEOUT_MS = 10 * 60_000L
        private const val RAMP_MS = 20_000L
        private const val RAMP_STEPS = 20
        private const val START_VOLUME = 0.2f

        private val _ringing = MutableStateFlow<Ringing?>(null)
        val ringing: StateFlow<Ringing?> = _ringing.asStateFlow()

        fun serviceIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, AlarmService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** Snooze or stop from the alarm screen. */
        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmService::class.java).setAction(action))
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Voice alarms ringing now"
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
    }
}
