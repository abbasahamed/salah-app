package com.salah.app.calendar

import com.salah.location.LocationPreferences
import com.salah.location.SalahLocation
import com.salah.preferences.PrayerPreferences
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

internal interface CalendarRepository {
    val selectedLocation: Flow<SalahLocation?>
    val prayerSettings: Flow<PrayerSettings>
    fun schedule(request: PrayerCalculationRequest, refresh: Boolean = false): Flow<PrayerScheduleResult>
}

@Singleton
internal class DefaultCalendarRepository @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val prayerPreferences: PrayerPreferences,
    private val locationPreferences: LocationPreferences,
) : CalendarRepository {
    override val selectedLocation: Flow<SalahLocation?> = locationPreferences.selected
    override val prayerSettings: Flow<PrayerSettings> = prayerPreferences.settings
    override fun schedule(request: PrayerCalculationRequest, refresh: Boolean): Flow<PrayerScheduleResult> =
        prayerTimeRepository.schedule(request, refresh)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CalendarModule {
    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: DefaultCalendarRepository): CalendarRepository
}

