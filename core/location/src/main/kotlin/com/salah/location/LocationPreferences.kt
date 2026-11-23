package com.salah.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.locationStore by preferencesDataStore("location")

@Singleton
class LocationPreferences internal constructor(private val store: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.locationStore)

    val selected = store.data.retryWhen { cause, _ ->
        if (cause !is IOException) return@retryWhen false
        emit(emptyPreferences())
        delay(2_000)
        true
    }.map(::decode).distinctUntilChanged()

    suspend fun select(location: SalahLocation?) {
        store.edit { values ->
            values.clear()
            if (location == null) return@edit
            values[mode] = location.mode.name
            values[latitude] = location.coordinates.latitude
            values[longitude] = location.coordinates.longitude
            values[zone] = location.zone.id
            location.id?.let { values[id] = it }
            location.names.locality?.let { values[locality] = it }
            location.names.region?.let { values[region] = it }
            location.names.country?.let { values[country] = it }
            location.accuracy?.let {
                values[approximate] = it.approximate
                it.meters?.let { meters -> values[accuracy] = meters }
            }
            location.observedAt?.let { values[observed] = it.toEpochMilli() }
            values[stale] = location.stale
        }
    }

    private fun decode(values: Preferences): SalahLocation? = runCatching {
        SalahLocation(
            id = values[id],
            coordinates = LocationCoordinates(requireNotNull(values[latitude]), requireNotNull(values[longitude])),
            names = PlaceNames(values[locality], values[region], values[country]),
            zone = ZoneId.of(requireNotNull(values[zone])),
            mode = LocationMode.valueOf(requireNotNull(values[mode])),
            accuracy = values[approximate]?.let { LocationAccuracy(it, values[accuracy]) },
            observedAt = values[observed]?.let(Instant::ofEpochMilli),
            stale = values[stale] ?: false,
        )
    }.getOrNull()

    private companion object {
        val mode = stringPreferencesKey("mode")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val zone = stringPreferencesKey("zone")
        val id = stringPreferencesKey("id")
        val locality = stringPreferencesKey("locality")
        val region = stringPreferencesKey("region")
        val country = stringPreferencesKey("country")
        val approximate = booleanPreferencesKey("approximate")
        val accuracy = floatPreferencesKey("accuracy")
        val observed = longPreferencesKey("observed")
        val stale = booleanPreferencesKey("stale")
    }
}
