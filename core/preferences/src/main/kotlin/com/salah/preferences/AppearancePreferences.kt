package com.salah.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode(val storedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
}

internal val Context.salahPreferences by preferencesDataStore("salah")
private val themeKey = stringPreferencesKey("theme_mode")

@Singleton
class AppearancePreferences internal constructor(
    private val store: DataStore<Preferences>,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.salahPreferences)

    val themeMode = store.data.catch { failure ->
        if (failure is IOException) emit(emptyPreferences()) else throw failure
    }.map { preferences ->
        ThemeMode.entries.firstOrNull { it.storedValue == preferences[themeKey] }
            ?: ThemeMode.SYSTEM
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.storedValue }
    }
}
