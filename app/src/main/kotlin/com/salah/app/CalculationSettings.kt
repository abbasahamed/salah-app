package com.salah.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salah.designsystem.SalahCard
import com.salah.designsystem.SectionHeading
import com.salah.prayer.AsrJuristicMethod
import com.salah.prayer.PrayerCalculationMethod
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerTimeFormat

@Composable
internal fun CalculationSettings(viewModel: TodayViewModel) {
    val persisted by viewModel.settings.collectAsStateWithLifecycle()
    val saveFailed by viewModel.settingsError.collectAsStateWithLifecycle()
    val settings = persisted ?: return
    val schools = AsrJuristicMethod.entries.associateWith {
        stringResource(if (it == AsrJuristicMethod.STANDARD) R.string.asr_standard else R.string.asr_hanafi)
    }
    val formats = PrayerTimeFormat.entries.associateWith {
        stringResource(if (it == PrayerTimeFormat.TWELVE_HOUR) R.string.format_12 else R.string.format_24)
    }

    val methodDisplayNames = PrayerCalculationMethod.entries.associateWith { stringResource(it.shortDisplayLabel()) }
    val methodMenuNames = PrayerCalculationMethod.entries.associateWith { stringResource(it.label()) }

    SectionHeading(stringResource(R.string.prayer_calculation))
    SalahCard(gap = 0.dp, inset = 14.dp) {
        ChoiceMenu(
            stringResource(R.string.calculation_method),
            settings.method,
            methodDisplayNames,
            methodMenuNames,
            viewModel::selectMethod,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ChoiceMenu(stringResource(R.string.asr_method), settings.asr, schools, schools, viewModel::selectAsr)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ChoiceMenu(stringResource(R.string.time_format), settings.format, formats, formats, viewModel::selectFormat)
    }

    SectionHeading(stringResource(R.string.minute_adjustments))
    SalahCard(gap = 0.dp, inset = 14.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth.value / LocalDensity.current.fontScale < 248
            Column {
                PrayerName.entries.forEachIndexed { index, prayer ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    AdjustmentRow(prayer, settings.adjustments[prayer], stacked, viewModel::adjust)
                }
            }
        }
        if (saveFailed) {
            Text(stringResource(R.string.settings_save_error), color = MaterialTheme.colorScheme.error)
        }
    }

    SectionHeading(stringResource(R.string.information))
    SalahCard(inset = 14.dp) {
        Text(stringResource(settings.method.label()), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.provider_information),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AdjustmentRow(
    prayer: PrayerName,
    value: Int,
    stacked: Boolean,
    adjust: (PrayerName, Int) -> Unit,
) {
    if (stacked) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            AdjustmentLabel(prayer)
            AdjustmentControls(prayer, value, adjust)
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.weight(1f)) { AdjustmentLabel(prayer) }
            AdjustmentControls(prayer, value, adjust)
        }
    }
}

@Composable
private fun AdjustmentLabel(prayer: PrayerName) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(SalahIcons.prayer(prayer), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(prayer.label()), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AdjustmentControls(prayer: PrayerName, value: Int, adjust: (PrayerName, Int) -> Unit) {
    val name = stringResource(prayer.label())
    Row(verticalAlignment = Alignment.CenterVertically) {
        AdjustmentButton(-1, value > -60, stringResource(R.string.decrease_adjustment, name)) { adjust(prayer, -1) }
        Text(
            stringResource(R.string.adjustment_value, value),
            Modifier.widthIn(min = 63.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        )
        AdjustmentButton(1, value < 60, stringResource(R.string.increase_adjustment, name)) { adjust(prayer, 1) }
    }
}

@Composable
private fun AdjustmentButton(delta: Int, enabled: Boolean, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (delta < 0) SalahIcons.Minus else SalahIcons.Plus,
                label,
                Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f),
            )
        }
    }
}

@Composable
private fun <T> ChoiceMenu(
    label: String,
    selected: T,
    displayOptions: Map<T, String>,
    menuOptions: Map<T, String>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clickable(
                        role = androidx.compose.ui.semantics.Role.Button,
                        onClick = { expanded = true },
                    )
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        displayOptions.getValue(selected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        SalahIcons.Chevron,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                menuOptions.forEach { (value, description) ->
                    DropdownMenuItem(
                        text = { Text(description, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.semantics { this.selected = value == selected },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        },
                    )
                }
            }
        }
    }
}
