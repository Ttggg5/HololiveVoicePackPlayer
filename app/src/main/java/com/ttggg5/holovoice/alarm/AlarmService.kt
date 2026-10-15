package com.ttggg5.holovoice.alarm

import androidx.core.content.ContextCompat
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
                if (!startInForeground(notification(null, null, null))) {
                    // Not allowed to ring from here: hand over to the alarm screen.
                    if (id >= 0) showFallback(this, id)
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (id >= 0) ring(id) else stopRinging()
            }
            ACTION_SNOOZE -> snoozeAndStop()
            ACTION_STOP -> {
                AlarmLog.record(this, "Stopped")
                stopRinging()
            }
            else -> if (ringingId < 0) stopSelf()
        }
        return START_NOT_STICKY
    }

    /** Goes to the foreground; false if Android doesn't allow it right now. */
    private fun startInForeground(notification: Notification): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        true
    } catch (e: Exception) {
        AlarmLog.record(this, "Couldn't start ringing in the foreground", e)
        false
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
            try {
                val repo = appContainer.alarms
                val alarm = repo.get(alarmId)
                if (alarm == null) {
                    AlarmLog.record(this@AlarmService, "Alarm $alarmId no longer exists")
                    stopRinging()
                    return@launch
                }
                runCatching { repo.onRang(alarm) }
                val clip = runCatching { repo.pickClip(alarm) }.getOrNull()
                val picture = withContext(Dispatchers.IO) { clip?.let(::loadPicture) }
                _ringing.value = Ringing(alarm, clip)
                runCatching {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(alarm, clip, picture))
                }
                AlarmLog.record(this@AlarmService, "Ringing with " + (clip?.let { "\"${it.clip.title}\"" } ?: "the alarm tone (no clip)"))
                play(clip?.clip?.fileUri?.let(Uri::parse))
            } catch (e: Exception) {
                AlarmLog.record(this@AlarmService, "Error while starting to ring", e)
                // Whatever went wrong, still wake the user: the system alarm sound.
                if (player == null) runCatching { play(null) }
            }
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
                AlarmLog.record(this@AlarmService, "Couldn't play the clip (${error.errorCodeName}); using the alarm tone")
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
        AlarmLog.record(this, "Snoozed")
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
        val open: () -> java.io.InputStream? = when (uri.scheme) {
            "file" -> { { File(uri.path!!).inputStream() } }
            "content" -> { { contentResolver.openInputStream(uri) } }
            else -> return null
        }
        // Read the size first, then decode small enough for a notification (a huge cover can't run out of memory).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= PICTURE_PX && bounds.outHeight / (sample * 2) >= PICTURE_PX) sample *= 2
        open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
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
        private const val PICTURE_PX = 256
        private const val FALLBACK_NOTIFICATION_ID = 7302
        private const val RING_CHANNEL_ID = "alarm_ring"

        private val _ringing = MutableStateFlow<Ringing?>(null)
        val ringing: StateFlow<Ringing?> = _ringing.asStateFlow()

        fun serviceIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, AlarmService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /**
         * Used when the ringing service can't start from the background: a full-screen notification
         * opens the alarm screen, which then starts the ringing itself.
         */
        fun showFallback(context: Context, alarmId: Long) {
            try {
                ensureRingChannel(context)
                val open = PendingIntent.getActivity(
                    context,
                    alarmId.toInt(),
                    Intent(context, AlarmActivity::class.java)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                fun action(action: String) = PendingIntent.getBroadcast(
                    context,
                    action.hashCode(),
                    Intent(context, AlarmReceiver::class.java).setAction(action).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val notification = NotificationCompat.Builder(context, RING_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_holovoice)
                    .setContentTitle("Alarm")
                    .setContentText("Tap to open the alarm and hear the voice")
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setContentIntent(open)
                    .setFullScreenIntent(open, true)
                    .addAction(0, "Snooze", action(AlarmReceiver.ACTION_SNOOZE_FALLBACK))
                    .addAction(0, "Stop", action(AlarmReceiver.ACTION_STOP_FALLBACK))
                    .build()
                    // Keep ringing (the channel's alarm tone) until it's handled.
                    .apply { flags = flags or Notification.FLAG_INSISTENT }
                context.getSystemService(NotificationManager::class.java).notify(FALLBACK_NOTIFICATION_ID, notification)
                AlarmLog.record(context, "Ringing with the alarm tone (notification)")
            } catch (e: Exception) {
                AlarmLog.record(context, "Couldn't show the alarm notification", e)
            }
        }

        fun cancelFallback(context: Context) {
            context.getSystemService(NotificationManager::class.java).cancel(FALLBACK_NOTIFICATION_ID)
        }

        /** A loud channel for [showFallback]: the phone's alarm tone at alarm volume, with vibration. */
        private fun ensureRingChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(RING_CHANNEL_ID) != null) return
            val tone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            manager.createNotificationChannel(
                NotificationChannel(RING_CHANNEL_ID, "Alarm ringing", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Rings when an alarm can't start its voice clip by itself"
                    setSound(
                        tone,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 400, 600)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }

        /** Starts ringing [alarmId] from the alarm screen (after [showFallback]). */
        fun ringFromScreen(context: Context, alarmId: Long) {
            cancelFallback(context)
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmService::class.java).setAction(ACTION_RING).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
                )
                AlarmLog.record(context, "Alarm screen started the voice clip")
            } catch (e: Exception) {
                AlarmLog.record(context, "Alarm screen couldn't start the voice clip", e)
            }
        }

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
