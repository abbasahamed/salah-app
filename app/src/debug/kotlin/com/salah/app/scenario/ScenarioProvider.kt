package com.salah.app.scenario

import android.content.Intent

object ScenarioProvider {
    fun getScenario(intent: Intent?): String? {
        return intent?.getStringExtra("scenario")
    }

    fun getThemeOverride(intent: Intent?): String? {
        return intent?.getStringExtra("theme")
    }
}

