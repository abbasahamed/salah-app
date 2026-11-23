package com.salah.app

import android.content.Context
import com.salah.prayer.PrayerTimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PrayerModule {
    @Provides
    fun clock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun repository(@ApplicationContext context: Context, clock: Clock): PrayerTimeRepository =
        PrayerTimeRepository(context, clock)
}
