package com.salah.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppearancePreferencesTest {
    @get:Rule
    val directory = TemporaryFolder()

    @Test
    fun missingAndUnknownValuesFollowSystemTheme() = runTest {
        val path = directory.newFolder().resolve("salah.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { path }
        val preferences = AppearancePreferences(store)

        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
        store.edit { it[stringPreferencesKey("theme_mode")] = "unknown" }
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
    }

    @Test
    fun themeSurvivesReopeningDataStore() = runTest {
        val path = directory.newFolder().resolve("salah.preferences_pb")
        val writerJob = Job(backgroundScope.coroutineContext[Job])
        val writerScope = CoroutineScope(backgroundScope.coroutineContext + writerJob)
        val writer = AppearancePreferences(PreferenceDataStoreFactory.create(scope = writerScope) { path })
        writer.setThemeMode(ThemeMode.DARK)
        writerJob.cancelAndJoin()

        val reader = AppearancePreferences(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { path },
        )
        assertEquals(ThemeMode.DARK, reader.themeMode.first())
    }

    @Test
    fun allModesRoundTripThroughDataStore() = runTest {
        val path = directory.newFolder().resolve("salah.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { path }
        val preferences = AppearancePreferences(store)

        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)) {
            preferences.setThemeMode(mode)
            assertEquals(mode, preferences.themeMode.first())
            assertEquals(mode.storedValue, store.data.first()[stringPreferencesKey("theme_mode")])
        }
    }
}
