package com.salah.app.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salah.app.R
import com.salah.app.SalahIcons
import com.salah.designsystem.SalahCard
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(
    viewModel: QiblaViewModel,
    onOpenLocation: () -> Unit,
    modifier: Modifier = Modifier,
    overrideState: QiblaUiState? = null,
) {
    val liveState by viewModel.state.collectAsStateWithLifecycle()
    val state = overrideState ?: liveState
    val hapticFeedback = LocalHapticFeedback.current

    if (overrideState == null) {
        androidx.lifecycle.compose.LifecycleResumeEffect(viewModel) {
            viewModel.startOrientationUpdates()
            onPauseOrDispose {
                viewModel.stopOrientationUpdates()
            }
        }
    }

    // Trigger subtle haptic feedback once upon entering the confirmed aligned state
    var wasFacing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isFacingQibla) {
        if (state.isFacingQibla && !wasFacing) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasFacing = state.isFacingQibla
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("qibla-screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val fontScale = density.fontScale

        // Dynamically compute responsive dial size so the compass visually dominates the screen:
        // Reference phones (390-412dp): ~312dp
        // Narrow phones (320dp): ~280dp
        // Landscape / low height: ~240-260dp
        // Accessibility large font scales: ~240-270dp
        val maxDialWidth = screenWidth - 32.dp
        val targetDialSize = when {
            screenHeight < 500.dp -> min(screenHeight - 120.dp, 230.dp)
            fontScale >= 1.8f -> min(maxDialWidth, 210.dp)
            fontScale >= 1.3f -> min(maxDialWidth, 240.dp)
            screenWidth < 360.dp -> min(maxDialWidth, 240.dp)
            else -> min(maxDialWidth, 270.dp)
        }

        // Available usable vertical space between header (~90dp) and bottom dock clearance (96dp)
        // Centers the hero dial on tall screens while permitting scrolling on short or high-font screens
        val usableMinHeight = max(0.dp, screenHeight - 195.dp)

        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val locState = state.locationState) {
                is QiblaLocationState.NoLocation -> {
                    // Header when no location configured
                    QiblaSimpleHeader(
                        locationName = null,
                        isManual = false,
                        bearing = null,
                    )
                    Spacer(Modifier.height(32.dp))
                    QiblaNoLocationCard(onOpenLocation = onOpenLocation)
                }
                is QiblaLocationState.Available -> {
                    val bearing = locState.bearing
                    val isSensorActive = state.compassState is CompassState.Active

                    // 1. Simplified, elegant header without redundant chips
                    QiblaSimpleHeader(
                        locationName = locState.locationName,
                        isManual = locState.isManual,
                        bearing = bearing,
                    )

                    // 2. Dominant Compass Hero Dial centered in usable vertical space
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = usableMinHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            QiblaCompass(
                                headingDegrees = state.smoothedHeadingDegrees,
                                qiblaBearingDegrees = bearing.bearingDegrees,
                                relativeAngleDegrees = state.relativeQiblaAngleDegrees,
                                isFacingQibla = state.isFacingQibla,
                                isSensorActive = isSensorActive,
                                modifier = Modifier.size(targetDialSize),
                                isLowAccuracy = state.needsCalibration,
                            )

                            // 3. Secondary guidance / advisories below compass
                            QiblaSecondaryGuidance(
                                state = state,
                                isSensorActive = isSensorActive,
                                isManual = locState.isManual,
                            )
                        }
                    }
                }
            }

            // Safe bottom clearance spacer ensuring content scrolls cleanly above floating navigation dock
            Spacer(Modifier.height(96.dp))
        }
    }
}

/**
 * Clean, simplified header:
 * - "Qibla"
 * - City name with subtle accessible location origin indicator
 * - Neutral white/gray degree metadata and distance (no gold)
 */
@Composable
private fun QiblaSimpleHeader(
    locationName: String?,
    isManual: Boolean,
    bearing: QiblaBearing?,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.qibla_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            color = colors.onBackground,
            modifier = Modifier.semantics { heading() },
        )

        if (locationName != null && bearing != null) {
            val locationSourceDesc = if (isManual) {
                stringResource(R.string.qibla_source_selected_city)
            } else {
                stringResource(R.string.qibla_source_device_gps)
            }

            // City name + subtle accessible affordance clarifying manual vs GPS coordinates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    ),
                    color = colors.onBackground,
                )

                Icon(
                    imageVector = if (isManual) SalahIcons.Place else SalahIcons.Qibla,
                    contentDescription = locationSourceDesc,
                    modifier = Modifier.size(13.dp),
                    tint = colors.onSurfaceVariant.copy(alpha = 0.50f),
                )
            }

            // Secondary metadata: "258° from north · 1,642 km" (neutral gray, NOT gold)
            val distanceStr = if (bearing.isAtKaaba) {
                stringResource(R.string.qibla_at_kaaba)
            } else {
                stringResource(R.string.qibla_distance_km_short, bearing.distanceKm.roundToInt())
            }

            Text(
                text = stringResource(
                    R.string.qibla_bearing_from_north,
                    bearing.bearingDegrees.roundToInt(),
                    distanceStr,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * Secondary status/guidance below the hero compass dial:
 * - Low-accuracy: figure-eight calibration advisory banner
 * - No-sensor: explanation to use bearing with separate compass
 * - Manual location & aligned: subtle note
 */
@Composable
private fun QiblaSecondaryGuidance(
    state: QiblaUiState,
    isSensorActive: Boolean,
    isManual: Boolean,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            !isSensorActive -> {
                Text(
                    text = stringResource(R.string.qibla_no_sensor_body_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            state.needsCalibration -> {
                Text(
                    text = stringResource(R.string.qibla_calibrate_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
            }
            state.isFacingQibla && isManual -> {
                Text(
                    text = stringResource(R.string.qibla_aligned_manual_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun QiblaNoLocationCard(onOpenLocation: () -> Unit) {
    SalahCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.qibla_no_location_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.qibla_no_location_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onOpenLocation) {
                Text(stringResource(R.string.choose_city))
            }
        }
    }
}
