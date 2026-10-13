package com.ttggg5.holovoice.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ttggg5.holovoice.MainActivity
import com.ttggg5.holovoice.data.Alarm
import java.time.LocalDateTime
import java.time.ZoneId

/** Puts alarms on Android's alarm clock (exact, shown in the status bar, allowed to wake the app). */
class AlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(AlarmManager::class.java)

    /** When [alarm] rings next (a pending snooze first), or null when it is off. */
    fun nextTriggerMs(alarm: Alarm, nowMs: Long = System.currentTimeMillis()): Long? {
        if (alarm.snoozeUntil > nowMs) return alarm.snoozeUntil
        if (!alarm.enabled) return null
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMs), zone)
        return AlarmTimes.next(alarm.hour, alarm.minute, alarm.days, now).atZone(zone).toInstant().toEpochMilli()
    }

    fun schedule(alarm: Alarm) {
        val trigger = nextTriggerMs(alarm)
        if (trigger == null) {
            cancel(alarm.id)
            return
        }
        val show = PendingIntent.getActivity(
            appContext,
            alarm.id.toInt(),
            Intent(appContext, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), fireIntent(alarm.id))
        } catch (e: SecurityException) {
            // Exact alarms not allowed (the user turned it off in system settings); nothing to schedule.
        }
    }

    fun cancel(alarmId: Long) {
        manager.cancel(fireIntent(alarmId))
    }

    private fun fireIntent(alarmId: Long): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        alarmId.toInt(),
        Intent(appContext, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
