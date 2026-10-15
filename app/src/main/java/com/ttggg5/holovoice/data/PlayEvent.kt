package com.ttggg5.holovoice.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One time a clip was played, and how long it was actually listened to (for listening stats). */
@Entity(
    tableName = "play_events",
    foreignKeys = [ForeignKey(entity = Clip::class, parentColumns = ["id"], childColumns = ["clipId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clipId"), Index("playedAt")],
)
data class PlayEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clipId: Long,
    val playedAt: Long,
    val listenedMs: Long = 0,
)

@Dao
interface PlayEventDao {
    @Insert
    suspend fun insert(event: PlayEvent): Long

    @Insert
    suspend fun insertAll(events: List<PlayEvent>)

    @Query("UPDATE play_events SET listenedMs = listenedMs + :ms WHERE id = :id")
    suspend fun addListened(id: Long, ms: Long)

    @Query("SELECT * FROM play_events ORDER BY playedAt")
    fun observeAll(): Flow<List<PlayEvent>>

    @Query("SELECT * FROM play_events ORDER BY playedAt")
    suspend fun getAll(): List<PlayEvent>

    @Query("DELETE FROM play_events")
    suspend fun clear()
}
