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
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (id < 0) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_RING).putExtra(EXTRA_ALARM_ID, id),
        )
    }

    companion object {
        const val ACTION_FIRE = "com.ttggg5.holovoice.alarm.FIRE"
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
