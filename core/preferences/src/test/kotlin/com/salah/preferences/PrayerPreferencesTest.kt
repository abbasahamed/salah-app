package com.salah.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.salah.prayer.AsrJuristicMethod
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerSettings
import com.salah.prayer.PrayerTimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrayerPreferencesTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun calculationSettingsAndThemeSurviveReopen() = runTest {
        val path = directory.newFolder().resolve("salah.preferences_pb")
        val writerJob = Job(backgroundScope.coroutineContext[Job])
        val scope = CoroutineScope(backgroundScope.coroutineContext + writerJob)
        val store = PreferenceDataStoreFactory.create(scope = scope) { path }
        val preferences = PrayerPreferences(store)
        assertEquals(PrayerSettings(), preferences.settings.first())
        AppearancePreferences(store).setThemeMode(ThemeMode.DARK)
        preferences.selectMethod(PrayerCalculationMethod.UMM_AL_QURA)
        preferences.selectAsr(AsrJuristicMethod.HANAFI)
        preferences.selectFormat(PrayerTimeFormat.TWELVE_HOUR)
        repeat(3) { preferences.adjust(PrayerName.FAJR, -1) }
        writerJob.cancelAndJoin()

        val reader = PreferenceDataStoreFactory.create(scope = backgroundScope) { path }
        val result = PrayerPreferences(reader).settings.first()
        assertEquals(PrayerCalculationMethod.UMM_AL_QURA, result.method)
        assertEquals(AsrJuristicMethod.HANAFI, result.asr)
        assertEquals(PrayerTimeFormat.TWELVE_HOUR, result.format)
        assertEquals(-3, result.adjustments.fajr)
        assertEquals(0, result.adjustments.asr)
        assertEquals(ThemeMode.DARK, AppearancePreferences(reader).themeMode.first())
    }
}
