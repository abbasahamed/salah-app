package com.salah.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.preferences.AppearancePreferences
import com.salah.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class AppearanceState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val saveFailed: Boolean = false,
    val saving: Boolean = false,
)

@HiltViewModel
internal class AppearanceViewModel @Inject constructor(
    private val preferences: AppearancePreferences,
) : ViewModel() {
    private val saveStatus = MutableStateFlow(SaveStatus.IDLE)

    val state = combine(preferences.themeMode, saveStatus) { mode, status ->
        AppearanceState(mode, status == SaveStatus.FAILED, status == SaveStatus.SAVING)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceState())

    fun selectTheme(mode: ThemeMode) {
        if (saveStatus.value == SaveStatus.SAVING) return
        saveStatus.value = SaveStatus.SAVING
        viewModelScope.launch {
            try {
                preferences.setThemeMode(mode)
                saveStatus.value = SaveStatus.IDLE
            } catch (_: IOException) {
                saveStatus.value = SaveStatus.FAILED
            }
        }
    }

    private enum class SaveStatus { IDLE, SAVING, FAILED }
}
