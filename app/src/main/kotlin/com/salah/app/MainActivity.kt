package com.salah.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salah.designsystem.SalahTheme
import com.salah.preferences.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SalahApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appearance by viewModels<AppearanceViewModel>()
    private val today by viewModels<TodayViewModel>()
    private val location by viewModels<LocationViewModel>()
    private val calendar by viewModels<com.salah.app.calendar.CalendarViewModel>()
    private val qibla by viewModels<com.salah.app.qibla.QiblaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = com.salah.app.scenario.ScenarioProvider.getScenario(intent)
        val themeOverride = com.salah.app.scenario.ScenarioProvider.getThemeOverride(intent)
        handlePinWidget(intent)
        enableEdgeToEdge()
        setContent {
            val state by appearance.state.collectAsStateWithLifecycle()
            val dark = when {
                themeOverride == "light" -> false
                themeOverride == "dark" -> true
                state.mode == ThemeMode.SYSTEM -> isSystemInDarkTheme()
                state.mode == ThemeMode.LIGHT -> false
                else -> true
            }
            SideEffect {
                val bars = SystemBarStyle.auto(
                    lightScrim = 0xE6EEEAE2.toInt(),
                    darkScrim = 0xE60A1118.toInt(),
                    detectDarkMode = { dark },
                )
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            SalahTheme(dark = dark) {
                SalahApp(state, appearance::selectTheme, today, location, calendar, qibla, scenario)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.salah.app.widget.SalahWidgetRefreshCoordinator.refreshAll(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handlePinWidget(intent)
    }

    private fun handlePinWidget(intent: android.content.Intent?) {
        val pinWidget = intent?.getStringExtra("pin_widget") ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = getSystemService(android.appwidget.AppWidgetManager::class.java)
            val provider = when (pinWidget) {
                "2x2" -> android.content.ComponentName(this, com.salah.app.widget.SalahNextPrayerWidgetReceiver::class.java)
                "4x2" -> android.content.ComponentName(this, com.salah.app.widget.SalahPrayerTimelineWidgetReceiver::class.java)
                else -> null
            }
            if (provider != null && appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(provider, null, null)
            }
        }
    }
}
