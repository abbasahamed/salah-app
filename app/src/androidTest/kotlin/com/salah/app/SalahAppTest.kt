package com.salah.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalahAppTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun todayShowsSixTimingsAndLocationReturnsToToday() {
        openManualPicker()
        chooseCity("Kattankulathur", "Tamil Nadu, India")
        awaitSchedule()
        val hasDialReadout = SemanticsMatcher("announces the next prayer and remaining time") { node ->
            node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
                .any { it.startsWith("Next prayer, ") && it.endsWith("remaining") }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasDialReadout).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasDialReadout).assertIsDisplayed()
        val timeFormat = Regex("\\d{2}:\\d{2}")
        val hasTime = SemanticsMatcher("has a 24-hour time") { node ->
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }
                .any { it.text.matches(timeFormat) }
        }
        listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha").forEach { entry ->
            compose.onNode(hasTestTag("prayer-$entry")).assertExists()
        }
        compose.onNode(hasTestTag("today-location")).performClick()
        compose.onNodeWithText("Use current location").assertIsDisplayed()
        compose.onNodeWithText("Search cities").assertIsDisplayed()
        searchCity("Kattankulathur")
        val selected = hasText("Kattankulathur") and isSelected()
        waitFor(selected)
        compose.onNode(selected).assertIsSelected()
        compose.onNodeWithContentDescription("Back").performClick()
        awaitSchedule()
    }

    @Test
    fun manualCitySwitchPersistsAndClearingReturnsToOnboarding() {
        openManualPicker()
        chooseCity("London", "England, United Kingdom")
        awaitSchedule()
        compose.onNode(hasTestTag("today-location")).performClick()
        chooseCity("Makkah", "Mecca Region, Saudi Arabia", query = "Mecca")
        awaitSchedule()
        assertBackgroundLocationPermissionAbsent()
        compose.activityRule.scenario.recreate()
        awaitSchedule()
        compose.onNodeWithText("Settings").performClick()
        val makkah = hasText("Makkah", substring = true)
        waitFor(makkah)
        compose.onNode(makkah).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear saved location").performScrollTo().performClick()
        compose.onNode(hasText("Clear saved location") and hasClickAction() and androidx.compose.ui.test.hasAnyAncestor(
            androidx.compose.ui.test.isDialog(),
        )).performClick()
        compose.onNodeWithText("Today").performClick()
        waitFor(hasText("Find your prayer times"))
        compose.onNodeWithText("Prayer times").assertDoesNotExist()
    }

    private fun assertBackgroundLocationPermissionAbsent() {
        val context = compose.activity
        val flags = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        org.junit.Assert.assertFalse(flags.requestedPermissions.orEmpty().contains("android.permission.ACCESS_BACKGROUND_LOCATION"))
    }

    private fun openManualPicker() {
        val manualMatcher = hasText("Choose a city") and hasClickAction()
        waitFor(manualMatcher or hasTestTag("today-location"))
        val manual = compose.onAllNodes(manualMatcher).fetchSemanticsNodes()
        if (manual.isNotEmpty()) compose.onNode(manualMatcher).performClick()
        else compose.onNode(hasTestTag("today-location")).performClick()
    }

    private fun searchCity(query: String) {
        val field = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
        field.performClick()
        field.performTextReplacement(query)
        field.performImeAction()
    }

    private fun chooseCity(name: String, qualifier: String, query: String = name) {
        searchCity(query)
        val city = hasText(name) and hasText(qualifier) and hasClickAction()
        waitFor(city)
        compose.onNode(city).performScrollTo().performClick()
    }

    private fun waitFor(matcher: SemanticsMatcher) = compose.waitUntil(12_000) {
        compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun tabsOpenCalendarAndPlaceholdersAndThemeSurvivesRecreation() {
        compose.onNodeWithText("Calendar").performClick()
        compose.onNode(hasTestTag("calendar-screen")).assertIsDisplayed()
        compose.onNodeWithText("Qibla").performClick()
        compose.onNode(hasTestTag("qibla-screen")).assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()

        try {
            selectTheme("Dark")
            compose.activityRule.scenario.recreate()
            awaitSelected("Dark")
            selectTheme("Light")
        } finally {
            selectTheme("System")
        }
    }

    @Test
    fun qiblaScreenDisplaysDirectionAndBearing() {
        openManualPicker()
        chooseCity("Dubai", "United Arab Emirates")
        awaitSchedule()
        compose.onNode(hasText("Qibla") and hasClickAction()).performClick()
        compose.onNode(hasTestTag("qibla-screen")).assertIsDisplayed()
        compose.onNode(hasText("Qibla") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertIsDisplayed()
        compose.onNodeWithText("Dubai").assertIsDisplayed()
        compose.onNodeWithContentDescription("Selected city").assertExists()
        // Bearing readout e.g. 258° or 259° from north
        compose.onNode(hasText("from north", substring = true)).assertExists()
    }

    @Test
    fun singleScreenDisplaysHeaderAllSixPrayersAndFitsAboveBottomNav() {
        openManualPicker()
        chooseCity("London", "England, United Kingdom")
        awaitSchedule()

        // 1. Assert hero and section heading are displayed
        compose.onNode(hasTestTag("today-location")).assertIsDisplayed()
        compose.onNodeWithText("Today’s prayer times").assertIsDisplayed()

        // 2. Assert all 6 prayer rows are displayed
        val prayerTags = listOf(
            "prayer-fajr",
            "prayer-sunrise",
            "prayer-dhuhr",
            "prayer-asr",
            "prayer-maghrib",
            "prayer-isha",
        )
        prayerTags.forEach { tag ->
            compose.onNode(hasTestTag(tag)).assertIsDisplayed()
        }

        // 3. Assert Isha row bottom bound is strictly < bottom-nav top bound (rendered above dock)
        val ishaBounds = compose.onNode(hasTestTag("prayer-isha")).getBoundsInRoot()
        val dockBounds = compose.onNode(hasTestTag("bottom-nav")).getBoundsInRoot()
        org.junit.Assert.assertTrue(
            "Isha row bottom (${ishaBounds.bottom}) must fit strictly above bottom nav top (${dockBounds.top})",
            ishaBounds.bottom < dockBounds.top,
        )
    }

    private fun awaitSchedule() {
        val hasSchedule = hasText("Today’s prayer times")
        compose.waitUntil(15_000) { compose.onAllNodes(hasSchedule).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun selectTheme(label: String) {
        compose.onNodeWithText(label).performClick()
        awaitSelected(label)
    }

    private fun selectThemeSafe(label: String) {
        compose.onNodeWithText(label).performClick()
        awaitSelected(label)
    }

    private fun awaitSelected(label: String) {
        val selected = hasText(label) and isSelected()
        compose.waitUntil(5_000) {
            compose.onAllNodes(selected).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(selected).assertIsSelected()
    }
}
