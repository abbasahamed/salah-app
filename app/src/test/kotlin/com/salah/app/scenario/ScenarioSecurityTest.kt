package com.salah.app.scenario

import org.junit.Assert.assertNull
import org.junit.Test

class ScenarioSecurityTest {

    @Test
    fun nullIntentReturnsNullScenarioAndTheme() {
        assertNull(ScenarioProvider.getScenario(null))
        assertNull(ScenarioProvider.getThemeOverride(null))
    }
}
