package com.salah.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.salah.prayer.AsrJuristicMethod
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeAdjustments
import com.salah.prayer.PrayerTimeFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val methodKey = stringPreferencesKey("prayer_method")
private val asrKey = stringPreferencesKey("prayer_asr")
private val formatKey = stringPreferencesKey("prayer_format")
private val adjustmentKeys = PrayerName.entries.associateWith { intPreferencesKey("prayer_adjustment_${it.name}") }

@Singleton
class PrayerPreferences internal constructor(private val store: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.salahPreferences)

    val settings = store.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map { values ->
        fun offset(prayer: PrayerName) = values[adjustmentKeys.getValue(prayer)]?.coerceIn(-60, 60) ?: 0
        PrayerSettings(
            method = PrayerCalculationMethod.entries.find { it.name == values[methodKey] }
                ?: PrayerCalculationMethod.KARACHI,
            asr = AsrJuristicMethod.entries.find { it.name == values[asrKey] } ?: AsrJuristicMethod.STANDARD,
            format = PrayerTimeFormat.entries.find { it.name == values[formatKey] } ?: PrayerTimeFormat.TWENTY_FOUR_HOUR,
            adjustments = PrayerTimeAdjustments(
                fajr = offset(PrayerName.FAJR),
                sunrise = offset(PrayerName.SUNRISE),
                dhuhr = offset(PrayerName.DHUHR),
                asr = offset(PrayerName.ASR),
                maghrib = offset(PrayerName.MAGHRIB),
                isha = offset(PrayerName.ISHA),
            ),
        )
    }.distinctUntilChanged()

    suspend fun selectMethod(value: PrayerCalculationMethod) { store.edit { it[methodKey] = value.name } }
    suspend fun selectAsr(value: AsrJuristicMethod) { store.edit { it[asrKey] = value.name } }
    suspend fun selectFormat(value: PrayerTimeFormat) { store.edit { it[formatKey] = value.name } }

    suspend fun adjust(prayer: PrayerName, delta: Int) {
        require(delta == -1 || delta == 1)
        val key = adjustmentKeys.getValue(prayer)
        store.edit { it[key] = ((it[key] ?: 0).coerceIn(-60, 60) + delta).coerceIn(-60, 60) }
    }
}
