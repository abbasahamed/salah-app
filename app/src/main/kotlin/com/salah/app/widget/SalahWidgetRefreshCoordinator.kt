package com.salah.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SalahWidgetRefreshCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                SalahNextPrayerWidget().updateAll(appContext)
                SalahPrayerTimelineWidget().updateAll(appContext)
            } catch (_: Exception) {
                // Ignore transient widget update exceptions
            }
        }
    }
}
