package com.salah.app.qibla

import com.salah.location.LocationPreferences
import com.salah.location.SalahLocation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface QiblaRepository {
    val selectedLocation: Flow<SalahLocation?>
}

@Singleton
class DefaultQiblaRepository @Inject constructor(
    private val locationPreferences: LocationPreferences,
) : QiblaRepository {
    override val selectedLocation: Flow<SalahLocation?> = locationPreferences.selected
}

@Module
@InstallIn(SingletonComponent::class)
abstract class QiblaRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindQiblaRepository(impl: DefaultQiblaRepository): QiblaRepository
}

