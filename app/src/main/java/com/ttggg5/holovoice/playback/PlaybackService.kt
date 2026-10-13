package com.ttggg5.holovoice.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.session.DefaultMediaNotificationProvider
import com.ttggg5.holovoice.R
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.ttggg5.holovoice.MainActivity
import com.ttggg5.holovoice.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the ExoPlayer in a [MediaSessionService] so playback continues in the background
 * and Media3 shows the standard media notification / lock screen controls. It also owns the
 * sleep timer and saves/restores the queue so playback can resume after the app is killed.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var store: PlaybackStateStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var restored: Deferred<MediaSession.MediaItemsWithStartPosition?>

    /** Until the saved queue has been restored, saving would overwrite it with an empty one. */
    private var restoreDone = false

    private var sleepEndsAtElapsed = 0L // SystemClock.elapsedRealtime() deadline, 0 = off
    private var sleepAtEndOfTrack = false

    private var loopStartMs = -1L
    private var loopEndMs = -1L
    private var loopMessage: PlayerMessage? = null

    private val sleepRunnable = Runnable {
        player.pause()
        clearSleepTimer()
    }

    private val periodicSave = object : Runnable {
        override fun run() {
            if (player.isPlaying) saveState()
            handler.postDelayed(this, SAVE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = PlaybackStateStore(this)
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(appContainer.uiPrefs.pauseOnUnplug.value)
            .build()
        player.addListener(PlayerListener())
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_PLAYER, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Media notification with the app's own small icon.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().also { it.setSmallIcon(R.drawable.ic_stat_holovoice) },
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            // Notification / lock-screen background: topic thumbnail with the talent picture on top.
            .setBitmapLoader(CollageBitmapLoader(this, DataSourceBitmapLoader(this)) { appContainer.uiPrefs.accent.value })
            .setCallback(Callback())
            .build()

        // Restore the last queue (paused) so the app and system "resume" controls pick up where we left off.
        // Follow the "Pause when headphones disconnect" setting while the service runs.
        scope.launch {
            appContainer.uiPrefs.pauseOnUnplug.state.collect { player.setHandleAudioBecomingNoisy(it) }
        }
        restored = scope.async { loadSaved() }
        scope.launch {
            val saved = restored.await()
            restoreDone = true
            if (saved != null && player.mediaItemCount == 0) {
                applySavedSettings()
                player.setMediaItems(saved.mediaItems, saved.startIndex, saved.startPositionMs)
                player.prepare()
            }
        }
        handler.postDelayed(periodicSave, SAVE_INTERVAL_MS)
    }

    private suspend fun loadSaved(): MediaSession.MediaItemsWithStartPosition? {
        // "Restore last queue on start" off: start empty (the queue is still saved for later).
        if (!appContainer.uiPrefs.resumeOnStart.value) return null
        val saved = store.load()
        if (saved.clipIds.isEmpty()) return null
        val clips = runCatching { appContainer.repository.clipsByIds(saved.clipIds) }.getOrDefault(emptyList())
        if (clips.isEmpty()) return null
        // Some clips may have been deleted since; if the saved index no longer points at the same clip, start over.
        val savedId = saved.clipIds.getOrNull(saved.index)
        val index = if (clips.getOrNull(saved.index)?.clip?.id == savedId) saved.index else 0
        val position = if (index == saved.index) saved.positionMs else 0L
        return MediaSession.MediaItemsWithStartPosition(clips.map { it.toMediaItem() }, index, position)
    }

    private fun saveState() {
        if (restoreDone) store.save(player)
    }

    private fun applySavedSettings() {
        val saved = store.load()
        player.shuffleModeEnabled = saved.shuffle
        player.repeatMode = saved.repeatMode
        player.setPlaybackSpeed(saved.speed)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveState()
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveState()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun setSleepTimer(minutes: Int, endOfTrack: Boolean) {
        handler.removeCallbacks(sleepRunnable)
        sleepEndsAtElapsed = 0L
        sleepAtEndOfTrack = endOfTrack
        player.pauseAtEndOfMediaItems = endOfTrack
        if (minutes > 0) {
            val delay = minutes * 60_000L
            sleepEndsAtElapsed = SystemClock.elapsedRealtime() + delay
            handler.postDelayed(sleepRunnable, delay)
        }
        publishExtras()
    }

    /** Repeats [startMs]..[endMs] of the current clip until cleared (-1) or the clip changes. */
    private fun setLoop(startMs: Long, endMs: Long) {
        loopMessage?.cancel()
        loopMessage = null
        if (startMs >= 0 && endMs > startMs && player.currentMediaItemIndex != C.INDEX_UNSET) {
            loopStartMs = startMs
            loopEndMs = endMs
            // Fires exactly when playback reaches B, also with the screen off.
            loopMessage = player.createMessage { _, _ -> player.seekTo(loopStartMs) }
                .setPosition(player.currentMediaItemIndex, endMs)
                .setLooper(Looper.getMainLooper())
                .setDeleteAfterDelivery(false)
                .send()
            if (player.currentPosition !in startMs..endMs) player.seekTo(startMs)
        } else {
            loopStartMs = -1L
            loopEndMs = -1L
        }
        publishExtras()
    }

    private fun clearSleepTimer() = setSleepTimer(minutes = 0, endOfTrack = false)

    /** Sleep timer and A–B loop state for the app's controller (extras replace each other, so both go together). */
    private fun publishExtras() {
        val endsAtWallClock = if (sleepEndsAtElapsed == 0L) {
            0L
        } else {
            System.currentTimeMillis() + (sleepEndsAtElapsed - SystemClock.elapsedRealtime())
        }
        session?.setSessionExtras(
            Bundle().apply {
                putLong(SleepTimer.EXTRA_ENDS_AT, endsAtWallClock)
                putBoolean(SleepTimer.EXTRA_END_OF_TRACK, sleepAtEndOfTrack)
                putLong(AbLoop.EXTRA_START, loopStartMs)
                putLong(AbLoop.EXTRA_END, loopEndMs)
            },
        )
    }

    private inner class PlayerListener : Player.Listener {
        /** Whether the current item has already been counted as played. */
        private var counted = false

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) || player.playbackState == Player.STATE_ENDED) {
                counted = false
            }
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) && loopEndMs >= 0) setLoop(-1L, -1L)
            if (player.isPlaying && !counted) {
                counted = true
                player.currentMediaItem?.mediaId?.toLongOrNull()?.let { id ->
                    scope.launch { runCatching { appContainer.repository.markPlayed(id) } }
                }
            }
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                saveState()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && sleepAtEndOfTrack) {
                clearSleepTimer()
            }
        }
    }

    private inner class Callback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(SleepTimer.COMMAND, Bundle.EMPTY))
                .add(SessionCommand(AbLoop.COMMAND, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            publishExtras()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == SleepTimer.COMMAND) {
                setSleepTimer(args.getInt(SleepTimer.ARG_MINUTES, 0), args.getBoolean(SleepTimer.ARG_END_OF_TRACK, false))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == AbLoop.COMMAND) {
                setLoop(args.getLong(AbLoop.ARG_START, -1L), args.getLong(AbLoop.ARG_END, -1L))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        /** Media items arrive from controllers without a playable URI; restore it from the request metadata. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
            mediaItems.map { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                if (uri == null) item else item.buildUpon().setUri(uri).build()
            }.toMutableList(),
        )

        /** Called by the system (e.g. Android 13+ media resumption) to restart the last session. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                val saved = restored.await()
                if (saved != null) {
                    applySavedSettings()
                    result.set(saved)
                } else {
                    result.setException(UnsupportedOperationException("Nothing to resume"))
                }
            }
            return result
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 10_000L
    }
}
