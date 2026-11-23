package com.salah.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.location.LocationFailure
import com.salah.location.LocationPermission
import com.salah.location.LocationPreferences
import com.salah.location.LocationRepository
import com.salah.location.LocationResolutionResult
import com.salah.location.LocationSearchResult
import com.salah.location.SalahLocation
import com.salah.location.toLocation
import com.salah.prayer.PrayerTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class LocationSelection(val ready: Boolean = false, val value: SalahLocation? = null)

internal sealed interface CitySearchState {
    data object Loading : CitySearchState
    data object Error : CitySearchState
    data class Results(val cities: List<LocationSearchResult>) : CitySearchState
}

internal data class LocationAction(
    val detecting: Boolean = false,
    val saving: Boolean = false,
    val failure: LocationFailure? = null,
    val stale: SalahLocation? = null,
    val saveError: Boolean = false,
    val completed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LocationViewModel @Inject constructor(
    private val repository: LocationRepository,
    private val preferences: LocationPreferences,
    private val prayerTimes: PrayerTimeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val selection = preferences.selected.map { LocationSelection(true, it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocationSelection())

    private val searches = MutableStateFlow("" to 0)
    val results = searches.flatMapLatest { (query, _) ->
        flow<Pair<String, CitySearchState>> {
            if (query.trim().length < 2) return@flow
            emit(query to CitySearchState.Loading)
            delay(300)
            val state = try {
                CitySearchState.Results(repository.search(query))
            } catch (_: IOException) {
                CitySearchState.Error
            }
            emit(query to state)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, 0), null)

    val action = MutableStateFlow(LocationAction())
    private var detection: Job? = null
    private var pickerGeneration = 0

    fun search(query: String) { searches.value = query to 0 }
    fun retrySearch() { searches.value = searches.value.let { it.first to it.second + 1 } }

    fun detect(permission: LocationPermission) {
        if (action.value.saving || action.value.detecting) return
        action.value = LocationAction(detecting = true)
        detection = viewModelScope.launch {
            when (val result = repository.detect(permission, selection.value.value)) {
                is LocationResolutionResult.Failed -> action.value = LocationAction(failure = result.reason)
                is LocationResolutionResult.Found -> if (result.location.stale) {
                    action.value = LocationAction(stale = result.location)
                } else {
                    save(result.location)
                }
            }
        }
    }

    fun choose(city: LocationSearchResult) {
        cancelDetection()
        save(city.toLocation())
    }

    fun useLastKnown() { action.value.stale?.let(::save) }

    fun clear() {
        cancelDetection()
        save(null)
    }

    fun cancelDetection() {
        if (!action.value.detecting) return
        detection?.cancel()
        action.value = LocationAction()
    }

    fun resetPicker() {
        pickerGeneration++
        cancelDetection()
        if (!action.value.saving) action.value = LocationAction()
        search("")
    }

    fun consumeCompletion() { action.value = action.value.copy(completed = false) }

    private fun save(value: SalahLocation?) {
        if (action.value.saving) return
        action.value = LocationAction(saving = true)
        val generation = pickerGeneration
        viewModelScope.launch {
            try {
                preferences.select(value)
                if (value == null) prayerTimes.clearLocationData()
                action.value = LocationAction(completed = value != null && generation == pickerGeneration)
                com.salah.app.widget.SalahWidgetRefreshCoordinator.refreshAll(context)
            } catch (_: IOException) {
                action.value = LocationAction(saveError = true)
            } catch (_: android.database.sqlite.SQLiteException) {
                action.value = LocationAction(saveError = true)
            }
        }
    }
}
