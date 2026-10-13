package com.ttggg5.holovoice.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** What an alarm plays. */
enum class AlarmSource(val label: String) {
    CLIP("Clip"),
    TALENT("Talent"),
    TOPIC("Topic"),
    FAVORITES("Favorites"),
}

/**
 * A voice alarm. [days] is a bitmask of weekdays (bit 0 = Monday … bit 6 = Sunday); 0 rings once.
 * [snoozeUntil] (epoch ms, 0 = none) survives reboots so a snoozed alarm still rings.
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val days: Int = 0,
    val enabled: Boolean = true,
    val label: String = "",
    val source: AlarmSource = AlarmSource.FAVORITES,
    val clipId: Long? = null,
    val talentSlug: String? = null,
    val packId: Long? = null,
    val snoozeMinutes: Int = 10,
    val snoozeUntil: Long = 0,
)

@Dao
abstract class AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    abstract fun observeAll(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms")
    abstract suspend fun getAll(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id")
    abstract suspend fun get(id: Long): Alarm?

    @Upsert
    abstract suspend fun upsert(alarm: Alarm): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    abstract suspend fun delete(id: Long)

    // Clips an alarm can pick from.

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.id = :id")
    abstract suspend fun clip(id: Long): ClipWithPack?

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.id IN (SELECT clipId FROM clip_talents WHERE slug = :slug)")
    abstract suspend fun clipsOfTalent(slug: String): List<ClipWithPack>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.packId = :packId")
    abstract suspend fun clipsOfTopic(packId: Long): List<ClipWithPack>

    @Transaction
    @Query("$CLIP_WITH_PACK WHERE clips.isFavorite = 1")
    abstract suspend fun favoriteClips(): List<ClipWithPack>

    @Transaction
    @Query("$CLIP_WITH_PACK ORDER BY RANDOM() LIMIT 1")
    abstract suspend fun anyClip(): ClipWithPack?
}
