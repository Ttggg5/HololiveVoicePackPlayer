package com.ttggg5.holovoice.alarm

import com.ttggg5.holovoice.data.Alarm
import com.ttggg5.holovoice.data.AlarmDao
import com.ttggg5.holovoice.data.AlarmSource
import com.ttggg5.holovoice.data.ClipWithPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Saves alarms and keeps Android's alarm clock in step with them. */
class AlarmRepository(private val dao: AlarmDao, val scheduler: AlarmScheduler) {
    val alarms: Flow<List<Alarm>> = dao.observeAll()

    /** A clip picked with "Set as alarm…"; the Alarms screen opens its editor with it. */
    val pendingClip = MutableStateFlow<Long?>(null)

    /** Set when the app is opened from the status-bar alarm icon. */
    val openRequested = MutableStateFlow(false)

    suspend fun get(id: Long): Alarm? = dao.get(id)

    suspend fun save(alarm: Alarm): Alarm {
        val id = dao.upsert(alarm.copy(snoozeUntil = 0))
        val saved = alarm.copy(id = if (alarm.id == 0L) id else alarm.id, snoozeUntil = 0)
        scheduler.schedule(saved)
        return saved
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) = save(alarm.copy(enabled = enabled))

    suspend fun delete(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        dao.delete(alarm.id)
    }

    suspend fun rescheduleAll() {
        dao.getAll().forEach(scheduler::schedule)
    }

    /** Called when [alarm] starts ringing: a one-time alarm turns off, a repeating one moves to its next day. */
    suspend fun onRang(alarm: Alarm) {
        val next = alarm.copy(snoozeUntil = 0, enabled = alarm.enabled && alarm.days != 0)
        dao.upsert(next)
        scheduler.schedule(next)
    }

    suspend fun snooze(alarmId: Long, nowMs: Long = System.currentTimeMillis()) {
        val alarm = dao.get(alarmId) ?: return
        val snoozed = alarm.copy(snoozeUntil = nowMs + alarm.snoozeMinutes * 60_000L)
        dao.upsert(snoozed)
        scheduler.schedule(snoozed)
    }

    /** The clip to ring with: from the alarm's source, else any clip in the library (null if empty). */
    suspend fun pickClip(alarm: Alarm): ClipWithPack? {
        val fromSource = when (alarm.source) {
            AlarmSource.CLIP -> alarm.clipId?.let { dao.clip(it) }
            AlarmSource.TALENT -> alarm.talentSlug?.let { dao.clipsOfTalent(it).randomOrNull() }
            AlarmSource.TOPIC -> alarm.packId?.let { dao.clipsOfTopic(it).randomOrNull() }
            AlarmSource.FAVORITES -> dao.favoriteClips().randomOrNull()
        }
        return fromSource ?: dao.anyClip()
    }

    /** The next alarm to ring and when, for the Alarms screen. */
    fun nextOf(alarms: List<Alarm>, nowMs: Long = System.currentTimeMillis()): Pair<Alarm, Long>? =
        alarms.mapNotNull { a -> scheduler.nextTriggerMs(a, nowMs)?.let { a to it } }.minByOrNull { it.second }
}
