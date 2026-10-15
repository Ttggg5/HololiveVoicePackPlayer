package com.ttggg5.holovoice.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ttggg5.holovoice.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Starts the ringing service when an alarm goes off. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        when (intent.action) {
            ACTION_FIRE -> if (id >= 0) fire(context, id)
            // Buttons on the fallback notification (when the ringing service wasn't allowed to start).
            ACTION_STOP_FALLBACK -> {
                AlarmService.cancelFallback(context)
                AlarmLog.record(context, "Stopped from the alarm notification")
            }
            ACTION_SNOOZE_FALLBACK -> {
                AlarmService.cancelFallback(context)
                AlarmLog.record(context, "Snoozed from the alarm notification")
                if (id >= 0) {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            context.appContainer.alarms.snooze(id)
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }
        }
    }

    private fun fire(context: Context, id: Long) {
        AlarmLog.record(context, "Alarm $id went off")
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_RING).putExtra(EXTRA_ALARM_ID, id),
            )
        } catch (e: Exception) {
            // Android refused to start the ringing service from the background: ring with the alarm
            // tone through a notification instead; opening it starts the voice clip.
            AlarmLog.record(context, "Android didn't allow the ringing service", e)
            AlarmService.showFallback(context, id)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.ttggg5.holovoice.alarm.FIRE"
        const val ACTION_STOP_FALLBACK = "com.ttggg5.holovoice.alarm.STOP_FALLBACK"
        const val ACTION_SNOOZE_FALLBACK = "com.ttggg5.holovoice.alarm.SNOOZE_FALLBACK"
        const val EXTRA_ALARM_ID = "alarmId"
    }
}

/** Alarms are lost on reboot and need new times after clock or time-zone changes. */
class AlarmRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.appContainer.alarms.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
