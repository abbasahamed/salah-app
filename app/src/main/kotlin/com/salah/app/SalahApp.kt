package com.salah.app

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salah.preferences.ThemeMode

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    TODAY("today", R.string.today, SalahIcons.Today),
    CALENDAR("calendar", R.string.calendar, SalahIcons.Calendar),
    QIBLA("qibla", R.string.qibla, SalahIcons.Qibla),
    SETTINGS("settings", R.string.settings, SalahIcons.Settings),
    LOCATION("location", R.string.location, SalahIcons.Place),
}

private val tabs = Destination.entries.filter { it != Destination.LOCATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalahApp(
    appearance: AppearanceState,
    onThemeSelected: (ThemeMode) -> Unit,
    today: TodayViewModel,
    location: LocationViewModel,
    calendar: com.salah.app.calendar.CalendarViewModel? = null,
    qibla: com.salah.app.qibla.QiblaViewModel? = null,
    scenario: String? = null,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = Destination.entries.firstOrNull { it.route == entry?.destination?.route?.substringBefore('?') }
        ?: Destination.TODAY
    val isLocation = current == Destination.LOCATION
    val openLocation: (Boolean) -> Unit = { detect ->
        nav.navigate("${Destination.LOCATION.route}?detect=$detect") { launchSingleTop = true }
    }
    val selection by location.selection.collectAsStateWithLifecycle()
    val selectTab: (Destination) -> Unit = { tab ->
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints {
        val showRail = maxWidth >= 600.dp && maxHeight >= 400.dp
        val showLabels = (maxWidth.value - 48f) / LocalDensity.current.fontScale >= 275f || showRail
        Row {
            if (showRail && !isLocation) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        val label = stringResource(tab.label)
                        NavigationRailItem(
                            selected = tab == current,
                            onClick = { selectTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    if (isLocation) {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(if (isLocation) R.string.location else R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            navigationIcon = {
                                if (isLocation) {
                                    IconButton(onClick = { nav.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                },
                bottomBar = {
                    if (!showRail && !isLocation) {
                        SalahBottomNavigation(current, showLabels, selectTab)
                    }
                },
            ) { insets ->
                Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                    val qiblaScenarioState = when (scenario) {
                        "qibla_aligned" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 258.2f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 258.2f,
                            relativeQiblaAngleDegrees = 0f,
                            isFacingQibla = true,
                            needsCalibration = false,
                        )
                        "qibla_turn_right" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 240.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 240.0f,
                            relativeQiblaAngleDegrees = 18.2f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_turn_left" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 270.2f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 270.2f,
                            relativeQiblaAngleDegrees = 348.0f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_low_accuracy" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 240.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.LOW,
                            ),
                            smoothedHeadingDegrees = 240.0f,
                            relativeQiblaAngleDegrees = 18.2f,
                            isFacingQibla = false,
                            needsCalibration = true,
                        )
                        "qibla_no_sensor" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.NoSensor,
                            smoothedHeadingDegrees = 0f,
                            relativeQiblaAngleDegrees = 258.2f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_manual_location" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 240.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 240.0f,
                            relativeQiblaAngleDegrees = 18.2f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_gps_location" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Dubai",
                                isManual = false,
                                bearing = com.salah.app.qibla.QiblaBearing(258.2f, 1642.0, false),
                                latitude = 25.2048,
                                longitude = 55.2708,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 258.2f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 258.2f,
                            relativeQiblaAngleDegrees = 0f,
                            isFacingQibla = true,
                            needsCalibration = false,
                        )
                        "qibla_angle_90" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Cairo",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(90.0f, 1280.0, false),
                                latitude = 30.0444,
                                longitude = 31.2357,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 0.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 0.0f,
                            relativeQiblaAngleDegrees = 90.0f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_angle_180" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Ankara",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(180.0f, 2150.0, false),
                                latitude = 39.9334,
                                longitude = 32.8597,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 0.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 0.0f,
                            relativeQiblaAngleDegrees = 180.0f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_angle_270" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Karachi",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(270.0f, 2800.0, false),
                                latitude = 24.8607,
                                longitude = 67.0011,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 0.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 0.0f,
                            relativeQiblaAngleDegrees = 270.0f,
                            isFacingQibla = false,
                            needsCalibration = false,
                        )
                        "qibla_angle_0" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Sanaa",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(0.0f, 850.0, false),
                                latitude = 15.3694,
                                longitude = 44.1910,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 0.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 0.0f,
                            relativeQiblaAngleDegrees = 0.0f,
                            isFacingQibla = true,
                            needsCalibration = false,
                        )
                        "qibla_angle_359" -> com.salah.app.qibla.QiblaUiState(
                            locationState = com.salah.app.qibla.QiblaLocationState.Available(
                                locationName = "Aden",
                                isManual = true,
                                bearing = com.salah.app.qibla.QiblaBearing(359.0f, 1020.0, false),
                                latitude = 12.7855,
                                longitude = 45.0187,
                            ),
                            compassState = com.salah.app.qibla.CompassState.Active(
                                headingDegrees = 0.0f,
                                accuracy = com.salah.app.qibla.SensorAccuracy.HIGH,
                            ),
                            smoothedHeadingDegrees = 0.0f,
                            relativeQiblaAngleDegrees = 359.0f,
                            isFacingQibla = true,
                            needsCalibration = false,
                        )
                        else -> null
                    }
                    val startDest = when {
                        scenario?.startsWith("qibla") == true -> Destination.QIBLA.route
                        scenario?.startsWith("calendar") == true -> Destination.CALENDAR.route
                        scenario?.startsWith("settings") == true -> Destination.SETTINGS.route
                        scenario?.startsWith("location") == true -> "${Destination.LOCATION.route}?detect=false"
                        else -> Destination.TODAY.route
                    }
                    NavHost(
                        navController = nav,
                        startDestination = startDest,
                    ) {
                        composable(Destination.TODAY.route) {
                            val state by today.state.collectAsStateWithLifecycle()
                            val todaySelection = when (scenario) {
                                "today_no_location" -> LocationSelection(ready = true, value = null)
                                "today_long_city" -> {
                                    val loc = com.salah.location.SalahLocation(
                                        id = "mock-long",
                                        coordinates = com.salah.location.LocationCoordinates(12.820254, 80.037819),
                                        names = com.salah.location.PlaceNames(
                                            locality = "Kattankulathur, Chengalpattu, Tamil Nadu",
                                            region = "Tamil Nadu",
                                            country = "India",
                                        ),
                                        zone = java.time.ZoneId.of("Asia/Kolkata"),
                                        mode = com.salah.location.LocationMode.MANUAL,
                                    )
                                    LocationSelection(ready = true, value = loc)
                                }
                                "today_gps" -> {
                                    val loc = com.salah.location.SalahLocation(
                                        id = "mock-gps",
                                        coordinates = com.salah.location.LocationCoordinates(25.2048, 55.2708),
                                        names = com.salah.location.PlaceNames(
                                            locality = "Dubai",
                                            region = "Dubai",
                                            country = "United Arab Emirates",
                                        ),
                                        zone = java.time.ZoneId.of("Asia/Dubai"),
                                        mode = com.salah.location.LocationMode.AUTOMATIC,
                                    )
                                    LocationSelection(ready = true, value = loc)
                                }
                                "today_offline", "today_after_isha" -> {
                                    val loc = selection.value ?: com.salah.location.SalahLocation(
                                        id = "mock-offline",
                                        coordinates = com.salah.location.LocationCoordinates(12.820254, 80.037819),
                                        names = com.salah.location.PlaceNames("Kattankulathur", "Tamil Nadu", "India"),
                                        zone = java.time.ZoneId.of("Asia/Kolkata"),
                                        mode = com.salah.location.LocationMode.MANUAL,
                                    )
                                    LocationSelection(ready = true, value = loc)
                                }
                                else -> selection
                            }
                            val selectedSite = todaySelection.value?.prayerLocation()
                            val heroVariant = if (scenario == "hero_variant_a") HeroSurfaceVariant.TONAL_CARD else HeroSurfaceVariant.CARDLESS

                            val overrideSchedule = when (scenario) {
                                "today_long_city", "today_gps" -> if (state.daily == null) {
                                    val loc = todaySelection.value!!
                                    val date = java.time.LocalDate.now(loc.zone)
                                    val req = com.salah.prayer.PrayerCalculationRequest(
                                        location = loc.prayerLocation(),
                                        date = date,
                                        method = com.salah.prayer.PrayerCalculationMethod.KARACHI,
                                        asr = com.salah.prayer.AsrJuristicMethod.STANDARD,
                                    )
                                    val timings = listOf(
                                        date.atTime(5, 12).atZone(loc.zone).toInstant(),
                                        date.atTime(6, 25).atZone(loc.zone).toInstant(),
                                        date.atTime(12, 18).atZone(loc.zone).toInstant(),
                                        date.atTime(15, 38).atZone(loc.zone).toInstant(),
                                        date.atTime(18, 8).atZone(loc.zone).toInstant(),
                                        date.atTime(19, 24).atZone(loc.zone).toInstant(),
                                    )
                                    val schedule = com.salah.prayer.DailyPrayerSchedule(req, timings)
                                    TodayState(
                                        date = date,
                                        daily = com.salah.prayer.PrayerScheduleResult.Available(schedule, com.salah.prayer.PrayerTimeSource.LOCAL_CACHE),
                                        next = com.salah.prayer.NextPrayer(
                                            time = schedule.timings[2],
                                            remaining = java.time.Duration.ofHours(3).plusMinutes(24),
                                        ),
                                        preceding = schedule.timings[1],
                                        site = loc.prayerLocation(),
                                    )
                                } else null
                                "today_offline" -> {
                                    val loc = todaySelection.value ?: com.salah.location.SalahLocation(
                                        id = "mock-offline",
                                        coordinates = com.salah.location.LocationCoordinates(12.820254, 80.037819),
                                        names = com.salah.location.PlaceNames("Kattankulathur", "Tamil Nadu", "India"),
                                        zone = java.time.ZoneId.of("Asia/Kolkata"),
                                        mode = com.salah.location.LocationMode.MANUAL,
                                    )
                                    val date = java.time.LocalDate.now(loc.zone)
                                    val req = com.salah.prayer.PrayerCalculationRequest(
                                        location = loc.prayerLocation(),
                                        date = date,
                                        method = com.salah.prayer.PrayerCalculationMethod.KARACHI,
                                        asr = com.salah.prayer.AsrJuristicMethod.STANDARD,
                                    )
                                    val timings = listOf(
                                        date.atTime(5, 12).atZone(loc.zone).toInstant(),
                                        date.atTime(6, 25).atZone(loc.zone).toInstant(),
                                        date.atTime(12, 18).atZone(loc.zone).toInstant(),
                                        date.atTime(15, 38).atZone(loc.zone).toInstant(),
                                        date.atTime(18, 8).atZone(loc.zone).toInstant(),
                                        date.atTime(19, 24).atZone(loc.zone).toInstant(),
                                    )
                                    val schedule = com.salah.prayer.DailyPrayerSchedule(req, timings)
                                    TodayState(
                                        date = date,
                                        daily = com.salah.prayer.PrayerScheduleResult.Available(schedule, com.salah.prayer.PrayerTimeSource.OFFLINE_CALCULATION),
                                        next = com.salah.prayer.NextPrayer(
                                            time = schedule.timings[2],
                                            remaining = java.time.Duration.ofHours(2).plusMinutes(15),
                                        ),
                                        preceding = schedule.timings[1],
                                        site = loc.prayerLocation(),
                                    )
                                }
                                "today_after_isha" -> {
                                    val loc = todaySelection.value ?: com.salah.location.SalahLocation(
                                        id = "mock-isha",
                                        coordinates = com.salah.location.LocationCoordinates(12.820254, 80.037819),
                                        names = com.salah.location.PlaceNames("Kattankulathur", "Tamil Nadu", "India"),
                                        zone = java.time.ZoneId.of("Asia/Kolkata"),
                                        mode = com.salah.location.LocationMode.MANUAL,
                                    )
                                    val date = java.time.LocalDate.now(loc.zone)
                                    val req = com.salah.prayer.PrayerCalculationRequest(
                                        location = loc.prayerLocation(),
                                        date = date,
                                        method = com.salah.prayer.PrayerCalculationMethod.KARACHI,
                                        asr = com.salah.prayer.AsrJuristicMethod.STANDARD,
                                    )
                                    val timings = listOf(
                                        date.atTime(5, 12).atZone(loc.zone).toInstant(),
                                        date.atTime(6, 25).atZone(loc.zone).toInstant(),
                                        date.atTime(12, 18).atZone(loc.zone).toInstant(),
                                        date.atTime(15, 38).atZone(loc.zone).toInstant(),
                                        date.atTime(18, 8).atZone(loc.zone).toInstant(),
                                        date.atTime(19, 24).atZone(loc.zone).toInstant(),
                                    )
                                    val todaySchedule = com.salah.prayer.DailyPrayerSchedule(req, timings)
                                    val tomorrowDate = date.plusDays(1)
                                    val tomorrowReq = req.copy(date = tomorrowDate)
                                    val tomorrowTimings = listOf(
                                        tomorrowDate.atTime(5, 13).atZone(loc.zone).toInstant(),
                                        tomorrowDate.atTime(6, 26).atZone(loc.zone).toInstant(),
                                        tomorrowDate.atTime(12, 18).atZone(loc.zone).toInstant(),
                                        tomorrowDate.atTime(15, 38).atZone(loc.zone).toInstant(),
                                        tomorrowDate.atTime(18, 8).atZone(loc.zone).toInstant(),
                                        tomorrowDate.atTime(19, 24).atZone(loc.zone).toInstant(),
                                    )
                                    val tomorrowSchedule = com.salah.prayer.DailyPrayerSchedule(tomorrowReq, tomorrowTimings)
                                    val tomorrowFajr = tomorrowSchedule.timings[0]
                                    TodayState(
                                        date = date,
                                        daily = com.salah.prayer.PrayerScheduleResult.Available(todaySchedule, com.salah.prayer.PrayerTimeSource.LOCAL_CACHE),
                                        next = com.salah.prayer.NextPrayer(
                                            time = tomorrowFajr,
                                            remaining = java.time.Duration.ofHours(7).plusMinutes(42),
                                        ),
                                        preceding = todaySchedule.timings.last(),
                                        site = loc.prayerLocation(),
                                    )
                                }
                                else -> null
                            }

                            val todayState = overrideSchedule ?: if (scenario == "unavailable") {
                                TodayState(
                                    date = state.date,
                                    daily = com.salah.prayer.PrayerScheduleResult.Unavailable(retryable = true),
                                    site = selectedSite,
                                )
                            } else {
                                state.takeIf { it.site == selectedSite } ?: TodayState(site = selectedSite)
                            }
                            TodayScreen(
                                todayState,
                                todaySelection, { openLocation(false) }, { openLocation(true) }, today::retry,
                                heroSurfaceVariant = heroVariant,
                            )
                        }
                        composable(Destination.CALENDAR.route) {
                            val calendarScenarioState = when (scenario) {
                                "calendar_offline" -> {
                                    val date = java.time.LocalDate.now()
                                    val zone = java.time.ZoneId.of("Asia/Kolkata")
                                    val loc = com.salah.prayer.PrayerLocation(12.820254, 80.037819, zone)
                                    val req = com.salah.prayer.PrayerCalculationRequest(
                                        location = loc,
                                        date = date,
                                        method = com.salah.prayer.PrayerCalculationMethod.KARACHI,
                                        asr = com.salah.prayer.AsrJuristicMethod.STANDARD,
                                    )
                                    val timings = listOf(
                                        date.atTime(5, 12).atZone(zone).toInstant(),
                                        date.atTime(6, 25).atZone(zone).toInstant(),
                                        date.atTime(12, 18).atZone(zone).toInstant(),
                                        date.atTime(15, 38).atZone(zone).toInstant(),
                                        date.atTime(18, 8).atZone(zone).toInstant(),
                                        date.atTime(19, 24).atZone(zone).toInstant(),
                                    )
                                    val schedule = com.salah.prayer.DailyPrayerSchedule(req, timings)
                                    val ym = java.time.YearMonth.from(date)
                                    val monthData = com.salah.app.calendar.CalendarDateConverter.createMonthData(
                                        yearMonth = ym,
                                        selectedDate = date,
                                        todayDate = date,
                                    )
                                    com.salah.app.calendar.CalendarUiState(
                                        displayedMonth = ym,
                                        selectedDate = date,
                                        monthData = monthData,
                                        scheduleState = com.salah.app.calendar.CalendarScheduleState.Success(schedule, com.salah.prayer.PrayerTimeSource.OFFLINE_CALCULATION),
                                        hasLocation = true,
                                        locationName = "Kattankulathur",
                                        zone = zone,
                                    )
                                }
                                else -> null
                            }
                            if (calendar != null) {
                                com.salah.app.calendar.CalendarScreen(
                                    viewModel = calendar,
                                    onOpenLocation = { openLocation(false) },
                                    overrideState = calendarScenarioState,
                                )
                            } else {
                                val vm: com.salah.app.calendar.CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                                com.salah.app.calendar.CalendarScreen(
                                    viewModel = vm,
                                    onOpenLocation = { openLocation(false) },
                                    overrideState = calendarScenarioState,
                                )
                            }
                        }
                        composable(Destination.QIBLA.route) {
                            if (qibla != null) {
                                com.salah.app.qibla.QiblaScreen(
                                    viewModel = qibla,
                                    onOpenLocation = { openLocation(false) },
                                    overrideState = qiblaScenarioState,
                                )
                            } else {
                                val vm: com.salah.app.qibla.QiblaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                                com.salah.app.qibla.QiblaScreen(
                                    viewModel = vm,
                                    onOpenLocation = { openLocation(false) },
                                    overrideState = qiblaScenarioState,
                                )
                            }
                        }
                        composable(Destination.SETTINGS.route) {
                            SettingsScreen(appearance, onThemeSelected, { CalculationSettings(today) }) {
                                LocationSettings(selection, location, openLocation)
                            }
                        }
                        composable(
                            "${Destination.LOCATION.route}?detect={detect}",
                            arguments = listOf(navArgument("detect") { type = NavType.BoolType; defaultValue = false }),
                        ) { pickerEntry ->
                            LocationScreen(
                                selection.value, location, pickerEntry.arguments?.getBoolean("detect") == true,
                                onSelected = { nav.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalahBottomNavigation(
    current: Destination,
    showLabels: Boolean,
    onSelect: (Destination) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val outerRadius = 24.dp
        val insetPadding = 2.5.dp
        val innerRadius = outerRadius - insetPadding // Concentric geometry: 21.5.dp

        Surface(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().testTag("bottom-nav"),
            shape = RoundedCornerShape(outerRadius),
            color = colors.surfaceContainer,
            shadowElevation = 2.dp,
        ) {
            BoxWithConstraints(Modifier.padding(all = insetPadding)) {
                val slot = maxWidth / tabs.size
                val offset by animateDpAsState(
                    slot * tabs.indexOf(current).coerceAtLeast(0),
                    spring(dampingRatio = 0.85f, stiffness = 420f),
                    label = "Selected destination",
                )
                Box(
                    Modifier.matchParentSize(),
                ) {
                    val indicatorShape = RoundedCornerShape(innerRadius)
                    Box(
                        Modifier
                            .offset { IntOffset(offset.roundToPx(), 0) }
                            .width(slot)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .padding(all = 1.dp)
                            .background(colors.onSurface.copy(alpha = 0.08f), indicatorShape),
                    )
                }
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    tabs.forEach { tab ->
                        val label = stringResource(tab.label)
                        val selected = tab == current
                        var focused by remember { mutableStateOf(false) }
                        // Distinct in neutral tones without needing bright gold: onSurface vs onSurfaceVariant
                        val contentColor by animateColorAsState(
                            if (selected) colors.onSurface else colors.onSurfaceVariant,
                            tween(150),
                            label = "Tab content color",
                        )
                        val shape = RoundedCornerShape(innerRadius)
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            if (pressed) 0.95f else 1f,
                            spring(stiffness = 500f),
                            label = "Tab press",
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(shape)
                                .onFocusChanged { focused = it.isFocused }
                                .then(if (focused) Modifier.border(1.5.dp, colors.onSurface.copy(alpha = 0.3f), shape) else Modifier)
                                .selectable(
                                    selected = selected,
                                    onClick = { onSelect(tab) },
                                    role = Role.Tab,
                                    interactionSource = interaction,
                                    indication = androidx.compose.material3.ripple(),
                                )
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = if (showLabels) null else label,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                tint = contentColor,
                            )
                            if (showLabels) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        letterSpacing = 0.sp,
                                    ),
                                    color = contentColor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
