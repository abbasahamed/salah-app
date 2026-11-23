package com.salah.prayer

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.Instant

internal data class CachedSchedule(val schedule: DailyPrayerSchedule, val retrieved: Instant)

internal interface PrayerCache {
    suspend fun read(request: PrayerCalculationRequest): CachedSchedule?
    suspend fun clear()
    suspend fun write(schedule: CachedSchedule)
}

private const val CACHE_FORMAT = 1
private const val PROVIDER_ID = "aladhan"

internal fun PrayerCalculationRequest.cacheKey(): String = listOf(
    CACHE_FORMAT, PROVIDER_ID, date, location.latitude, location.longitude, location.zone.id,
    method.alAdhanId, asr.alAdhanId, "angle-based",
    PrayerName.entries.joinToString(",") { adjustments[it].toString() },
).joinToString("|")

@Entity(tableName = "prayer_schedules")
internal data class ScheduleRow(
    @PrimaryKey val key: String,
    val provider: String,
    val formatVersion: Int,
    val rawEpochSeconds: String,
    val retrievedEpochSeconds: Long,
)

@Dao
internal abstract class ScheduleDao {
    @Query("SELECT * FROM prayer_schedules WHERE `key` = :key")
    abstract suspend fun row(key: String): ScheduleRow?

    @Query("DELETE FROM prayer_schedules")
    abstract suspend fun clear()

    @Upsert
    protected abstract suspend fun upsert(row: ScheduleRow)

    @Query("DELETE FROM prayer_schedules WHERE `key` NOT IN (SELECT `key` FROM prayer_schedules ORDER BY retrievedEpochSeconds DESC, `key` LIMIT 16)")
    protected abstract suspend fun trim()

    @Transaction
    open suspend fun save(row: ScheduleRow) {
        upsert(row)
        trim()
    }
}

@Database(entities = [ScheduleRow::class], version = 1)
internal abstract class PrayerDatabase : RoomDatabase() {
    abstract fun schedules(): ScheduleDao
}

internal class RoomPrayerCache(context: Context) : PrayerCache {
    private val dao = Room.databaseBuilder(
        context.applicationContext, PrayerDatabase::class.java, "prayer-schedules.db",
    ).build().schedules()

    override suspend fun read(request: PrayerCalculationRequest): CachedSchedule? {
        val row = dao.row(request.cacheKey()) ?: return null
        if (row.provider != PROVIDER_ID || row.formatVersion != CACHE_FORMAT) return null
        return runCatching {
            val raw = row.rawEpochSeconds.split(",").map { Instant.ofEpochSecond(it.toLong()) }
            CachedSchedule(DailyPrayerSchedule(request, raw), Instant.ofEpochSecond(row.retrievedEpochSeconds))
        }.getOrNull()
    }

    override suspend fun clear() = dao.clear()

    override suspend fun write(schedule: CachedSchedule) {
        dao.save(
            ScheduleRow(
                schedule.schedule.request.cacheKey(), PROVIDER_ID, CACHE_FORMAT,
                schedule.schedule.rawTimings.joinToString(",") { it.epochSecond.toString() },
                schedule.retrieved.epochSecond,
            ),
        )
    }
}
