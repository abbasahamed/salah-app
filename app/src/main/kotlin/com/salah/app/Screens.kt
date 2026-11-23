package com.salah.app

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salah.designsystem.SalahCard
import com.salah.designsystem.SectionHeading
import com.salah.preferences.ThemeMode
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerName

@StringRes
internal fun PrayerName.label(): Int = when (this) {
    PrayerName.FAJR -> R.string.fajr
    PrayerName.SUNRISE -> R.string.sunrise
    PrayerName.DHUHR -> R.string.dhuhr
    PrayerName.ASR -> R.string.asr
    PrayerName.MAGHRIB -> R.string.maghrib
    PrayerName.ISHA -> R.string.isha
}

@StringRes
internal fun PrayerCalculationMethod.label(): Int = when (this) {
    PrayerCalculationMethod.KARACHI -> R.string.method_karachi
    PrayerCalculationMethod.ISNA -> R.string.method_isna
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
    PrayerCalculationMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
}

@StringRes
internal fun PrayerCalculationMethod.shortDisplayLabel(): Int = when (this) {
    PrayerCalculationMethod.KARACHI -> R.string.method_karachi_display
    PrayerCalculationMethod.ISNA -> R.string.method_isna_display
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl_display
    PrayerCalculationMethod.UMM_AL_QURA -> R.string.method_umm_al_qura_display
}

@Composable
internal fun SettingsScreen(
    appearance: AppearanceState,
    onThemeSelected: (ThemeMode) -> Unit,
    calculationContent: @Composable () -> Unit,
    locationContent: @Composable () -> Unit,
) {
    ScreenContent(R.string.settings) {
        SectionHeading(stringResource(R.string.appearance))
        AppearanceChoices(appearance, onThemeSelected)
        if (appearance.saveFailed) {
            Text(
                stringResource(R.string.appearance_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        calculationContent()
        locationContent()
    }
}

@Composable
private fun AppearanceChoices(state: AppearanceState, onSelect: (ThemeMode) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth.value / LocalDensity.current.fontScale >= 230
        val colors = MaterialTheme.colorScheme
        if (compact) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainer)
                    .selectableGroup()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    val selected = state.mode == mode
                    val fill = if (selected) colors.surface else Color.Transparent
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(fill)
                            .selectable(
                                selected = selected,
                                enabled = !state.saving,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) },
                            )
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(mode.label()),
                            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        } else {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val selected = state.mode == mode
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = selected,
                                enabled = !state.saving,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected, onClick = null, enabled = !state.saving)
                        Text(stringResource(mode.label()))
                    }
                }
            }
        }
    }
}

@StringRes
private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.system_theme
    ThemeMode.LIGHT -> R.string.light_theme
    ThemeMode.DARK -> R.string.dark_theme
}

@Composable
internal fun PlaceholderScreen(@StringRes title: Int, @StringRes message: Int) {
    ScreenContent(title) { SalahCard { Text(stringResource(message)) } }
}

@Composable
internal fun ScreenContent(@StringRes title: Int? = null, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (title != null) {
                Text(
                    stringResource(title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            content()
            // Safe bottom clearance spacer so all content scrolls completely above the floating navigation dock
            Spacer(Modifier.height(96.dp))
        }
    }
}
