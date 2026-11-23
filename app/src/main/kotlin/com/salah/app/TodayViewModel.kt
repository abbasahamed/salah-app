package com.salah.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.location.LocationPreferences
import com.salah.location.SalahLocation
import com.salah.preferences.PrayerPreferences
import com.salah.prayer.AsrJuristicMethod
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerTime
import com.salah.prayer.PrayerTimeFormat
import com.salah.prayer.NextPrayer
import com.salah.prayer.NextPrayerResolver
import com.salah.prayer.PrayerCalculationRequest
import com.salah.prayer.PrayerLocation
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal data class TodayState(
    val date: LocalDate? = null,
    val settings: PrayerSettings = PrayerSettings(),
    val daily: PrayerScheduleResult? = null,
    val next: NextPrayer? = null,
    val nextLoading: Boolean = false,
    val preceding: PrayerTime? = null,
    val site: PrayerLocation? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class TodayViewModel @Inject constructor(
    private val repository: PrayerTimeRepository,
    private val clock: Clock,
    private val preferences: PrayerPreferences,
    locationPreferences: LocationPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val resolver = NextPrayerResolver(clock)
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settingsError = MutableStateFlow(false)
    private val retry = MutableStateFlow(0)
    private var lastHandledRetry = 0
    private var lastHandledTomorrowRetry = 0
    private val subscription = SharingStarted.WhileSubscribed(0, 0)
    private val ticks = flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(1000L - now.nano / 1_000_000L)
        }
    }.shareIn(viewModelScope, subscription, replay = 1)

    val state = locationPreferences.selected.map { it?.prayerLocation() }.distinctUntilChanged()
        .flatMapLatest { site ->
            if (site == null) flowOf(TodayState()) else forLocation(site)
                .map { it.copy(site = site) }.onStart { emit(TodayState(site = site)) }
        }.stateIn(viewModelScope, subscription, TodayState())

    private fun forLocation(site: PrayerLocation): kotlinx.coroutines.flow.Flow<TodayState> {
        val dates = ticks.map { it.atZone(site.zone).toLocalDate() }.distinctUntilChanged()
        val requests = combine(dates, settings.filterNotNull(), retry) { date, config, attempt ->
            PrayerCalculationRequest(site, date, config.method, config.asr, config.adjustments) to attempt
        }.distinctUntilChanged()
        val daily = requests.flatMapLatest { (request, attempt) ->
            val force = attempt > 0 && lastHandledRetry != attempt
            lastHandledRetry = attempt
            repository.schedule(request, refresh = force)
                .map { ScheduleSnapshot(request, attempt, it) }
                .onStart { emit(ScheduleSnapshot(request, attempt, null)) }
        }
        val countdown = daily.flatMapLatest { (request, attempt, result) ->
            val available = result as? PrayerScheduleResult.Available
                ?: return@flatMapLatest flowOf(TodayState(request.date, daily = result))
            val schedule = available.schedule
            val previous = flow {
                emit(repository.localSchedule(request.copy(date = request.date.minusDays(1)))
                    as? PrayerScheduleResult.Available)
            }
            val nextDate = request.date.plusDays(1)
            val earliestFollowingPrayer = nextDate.atStartOfDay(site.zone).toInstant().minusSeconds(3600)
            val tomorrow = ticks.map {
                resolver.resolve(listOf(schedule), it) == null || it >= earliestFollowingPrayer
            }
                .distinctUntilChanged().flatMapLatest followingDay@{ needed ->
                    if (!needed) return@followingDay flowOf<PrayerScheduleResult?>(null)
                    val refresh = attempt > lastHandledTomorrowRetry
                    lastHandledTomorrowRetry = attempt
                    repository.schedule(request.copy(date = nextDate), refresh)
                        .map<PrayerScheduleResult, PrayerScheduleResult?> { it }.onStart { emit(null) }
                }
            combine(ticks, previous, tomorrow) { now, yesterday, following ->
                val candidates = listOfNotNull(
                    yesterday?.schedule, schedule, (following as? PrayerScheduleResult.Available)?.schedule,
                )
                val next = resolver.resolve(candidates, now)
                val preceding = candidates.asSequence().flatMap { it.timings.asSequence() }
                    .filter { it.name != PrayerName.SUNRISE && it.instant <= now }
                    .maxByOrNull { it.instant }
                TodayState(
                    request.date, daily = result, next = next,
                    nextLoading = next == null && following == null,
                    preceding = preceding,
                )
            }
        }
        return combine(countdown, settings.filterNotNull()) { snapshot, config -> snapshot.copy(settings = config) }
    }

    private data class ScheduleSnapshot(
        val request: PrayerCalculationRequest,
        val attempt: Int,
        val result: PrayerScheduleResult?,
    )

    fun retry() { retry.update { it + 1 } }

    fun selectMethod(value: PrayerCalculationMethod) = save { preferences.selectMethod(value) }
    fun selectAsr(value: AsrJuristicMethod) = save { preferences.selectAsr(value) }
    fun selectFormat(value: PrayerTimeFormat) = save { preferences.selectFormat(value) }
    fun adjust(prayer: PrayerName, delta: Int) = save { preferences.adjust(prayer, delta) }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                settingsError.value = false
                com.salah.app.widget.SalahWidgetRefreshCoordinator.refreshAll(context)
            } catch (_: IOException) {
                settingsError.value = true
            }
        }
    }
}

internal fun SalahLocation.prayerLocation() = PrayerLocation(coordinates.latitude, coordinates.longitude, zone)
