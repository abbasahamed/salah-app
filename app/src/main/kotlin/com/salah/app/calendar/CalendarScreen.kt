package com.salah.app.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salah.app.R
import com.salah.app.SalahIcons
import com.salah.app.label
import com.salah.designsystem.SalahCard
import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerTimeFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenLocation: () -> Unit,
    modifier: Modifier = Modifier,
    overrideState: CalendarUiState? = null,
) {
    val vmState by viewModel.state.collectAsStateWithLifecycle()
    val state = overrideState ?: vmState
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val screenWidthDp = configuration.screenWidthDp

    var showInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar-screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Refined Month Navigation Header
            CalendarMonthHeader(
                displayedMonth = state.displayedMonth,
                hijriMonthSummary = state.monthData.hijriMonthSummary,
                hijriMonthFullSummary = state.monthData.hijriMonthFullSummary,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onJumpToToday = viewModel::jumpToToday,
                onShowInfo = { showInfoDialog = true },
                fontScale = fontScale,
                screenWidthDp = screenWidthDp,
                locale = locale,
            )

            // 2. Cardless Seven-Column Gregorian Month Grid with horizontal swipe gesture
            CalendarMonthGrid(
                monthData = state.monthData,
                onDateSelected = viewModel::selectDate,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                fontScale = fontScale,
                locale = locale,
            )

            // 3. Selected-Date Prayer Schedule & Details (Grouped graphite card)
            SelectedDateScheduleSection(
                selectedDate = state.selectedDate,
                scheduleState = state.scheduleState,
                timeFormat = state.timeFormat,
                zone = state.zone,
                hasLocation = state.hasLocation,
                onOpenLocation = onOpenLocation,
                onRetry = viewModel::retry,
                locale = locale,
            )

            // 4. Discreet Calculation Footnote
            Text(
                text = stringResource(R.string.hijri_footnote),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
                    .clickable(onClick = { showInfoDialog = true }),
            )

            // 5. Safe bottom inset clearance ensuring Isha row scrolls well above floating navigation dock
            Spacer(Modifier.height(100.dp))
        }

        // Calculation Explanation Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.hijri_calculation_dialog_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.hijri_calculation_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text(
                            text = stringResource(R.string.close),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    displayedMonth: YearMonth,
    hijriMonthSummary: String,
    hijriMonthFullSummary: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onShowInfo: () -> Unit,
    fontScale: Float,
    screenWidthDp: Int,
    locale: Locale,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val gregorianMonthFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }
    val monthTitle = remember(displayedMonth, gregorianMonthFormatter) {
        gregorianMonthFormatter.format(displayedMonth)
    }

    // Adaptive layout for large fonts or narrow screens
    val isStackedLayout = fontScale >= 1.3f || screenWidthDp < 360

    if (isStackedLayout) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Row 1: Month Title & Hijri Subtitle
            MonthTitleAndSubtitle(
                monthTitle = monthTitle,
                hijriMonthSummary = hijriMonthSummary,
                hijriMonthFullSummary = hijriMonthFullSummary,
                onShowInfo = onShowInfo,
                colors = colors,
                type = type,
            )

            // Row 2: Navigation controls on dedicated row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthNavigationControls(
                    onJumpToToday = onJumpToToday,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    colors = colors,
                    type = type,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MonthTitleAndSubtitle(
                    monthTitle = monthTitle,
                    hijriMonthSummary = hijriMonthSummary,
                    hijriMonthFullSummary = hijriMonthFullSummary,
                    onShowInfo = onShowInfo,
                    colors = colors,
                    type = type,
                )
            }

            MonthNavigationControls(
                onJumpToToday = onJumpToToday,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                colors = colors,
                type = type,
            )
        }
    }
}

@Composable
private fun MonthTitleAndSubtitle(
    monthTitle: String,
    hijriMonthSummary: String,
    hijriMonthFullSummary: String,
    onShowInfo: () -> Unit,
    colors: androidx.compose.material3.ColorScheme,
    type: androidx.compose.material3.Typography,
) {
    Text(
        text = monthTitle,
        style = type.headlineMedium.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        color = colors.onSurface,
        modifier = Modifier.semantics { heading() },
    )

    if (hijriMonthSummary.isNotEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.hijri_calculation_notice),
                    onClick = onShowInfo,
                )
                .semantics {
                    contentDescription = hijriMonthFullSummary
                },
        ) {
            Text(
                text = hijriMonthSummary,
                style = type.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.onSurfaceVariant,
            )
            Icon(
                imageVector = SalahIcons.Info,
                contentDescription = stringResource(R.string.hijri_calculation_notice),
                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun MonthNavigationControls(
    onJumpToToday: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    colors: androidx.compose.material3.ColorScheme,
    type: androidx.compose.material3.Typography,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Restrained text action for Today
        TextButton(
            onClick = onJumpToToday,
            modifier = Modifier.heightIn(min = 40.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.jump_to_today),
                style = type.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.primary,
            )
        }

        // Previous Month Button (Accessible touch target)
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SalahIcons.ChevronLeft,
                contentDescription = stringResource(R.string.previous_month),
                tint = colors.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }

        // Next Month Button (Accessible touch target)
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = SalahIcons.ChevronRight,
                contentDescription = stringResource(R.string.next_month),
                tint = colors.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    monthData: CalendarMonthData,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    fontScale: Float,
    locale: Locale,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val density = LocalDensity.current
    val weekdays = remember(locale) { CalendarDateConverter.orderedWeekdays(locale) }

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    val dragThresholdPx = with(density) { 48.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(monthData.yearMonth) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDragX = 0f },
                    onDragEnd = {
                        if (accumulatedDragX > dragThresholdPx) {
                            onPreviousMonth()
                        } else if (accumulatedDragX < -dragThresholdPx) {
                            onNextMonth()
                        }
                        accumulatedDragX = 0f
                    },
                    onDragCancel = { accumulatedDragX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        accumulatedDragX += dragAmount
                        change.consume()
                    },
                )
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Weekday Column Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            weekdays.forEach { dayOfWeek ->
                val label = dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
                val isFriday = dayOfWeek == DayOfWeek.FRIDAY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = label,
                        style = type.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isFriday) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isFriday) colors.primary.copy(alpha = 0.9f) else colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (isFriday) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(colors.primary.copy(alpha = 0.85f), CircleShape),
                        )
                    }
                }
            }
        }

        // Weeks Rows
        monthData.weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        onDateSelected = onDateSelected,
                        fontScale = fontScale,
                        locale = locale,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    onDateSelected: (LocalDate) -> Unit,
    fontScale: Float,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val isSelected = day.isSelected
    val isToday = day.isToday
    val isCurrentMonth = day.isCurrentMonth

    val cellBackground by animateColorAsState(
        targetValue = when {
            isSelected -> colors.primary.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "cellBackground",
    )

    val cellBorder = when {
        isSelected -> BorderStroke(1.2.dp, colors.primary)
        !isSelected && isToday -> BorderStroke(1.dp, colors.primary.copy(alpha = 0.6f))
        else -> null
    }

    val gregorianTextColor by animateColorAsState(
        targetValue = when {
            isSelected -> colors.primary
            !isCurrentMonth -> colors.onSurface.copy(alpha = 0.22f)
            isToday -> colors.primary
            else -> colors.onSurface
        },
        animationSpec = tween(durationMillis = 150),
        label = "gregorianText",
    )

    val hijriTextColor by animateColorAsState(
        targetValue = when {
            isSelected -> colors.primary.copy(alpha = 0.85f)
            !isCurrentMonth -> colors.onSurfaceVariant.copy(alpha = 0.2f)
            day.isHijriMonthStart -> colors.primary
            else -> colors.onSurfaceVariant.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 150),
        label = "hijriText",
    )

    val gregorianWeight = when {
        isSelected -> FontWeight.SemiBold
        isToday -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }

    val hijriDayText = remember(day.hijriDate) {
        CalendarDateConverter.formatHijriDay(day.hijriDate)
    }

    val fullDateDescription = remember(day.date, day.hijriDate, isSelected, isToday, locale) {
        val gregorianFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale).format(day.date)
        val hijriFormat = CalendarDateConverter.formatHijriDateFull(day.hijriDate, locale)
        buildString {
            append(gregorianFormat)
            if (isToday) append(", today")
            if (isSelected) append(", selected")
            if (hijriFormat.isNotEmpty()) append(", $hijriFormat")
        }
    }

    val cellShape = RoundedCornerShape(8.dp)

    val cellModifier = if (isCurrentMonth) {
        modifier
            .heightIn(min = if (fontScale >= 1.5f) 52.dp else 48.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clip(cellShape)
            .background(cellBackground)
            .then(if (cellBorder != null) Modifier.border(cellBorder, cellShape) else Modifier)
            .clickable(
                role = Role.Button,
                onClick = { onDateSelected(day.date) },
            )
            .clearAndSetSemantics {
                contentDescription = fullDateDescription
                selected = isSelected
                role = Role.Button
            }
    } else {
        modifier
            .heightIn(min = if (fontScale >= 1.5f) 52.dp else 48.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clearAndSetSemantics {
                contentDescription = fullDateDescription
            }
    }

    val gregorianFontSize = when {
        fontScale >= 1.7f -> 12.sp
        fontScale >= 1.3f -> 13.5.sp
        else -> 15.sp
    }
    val hijriFontSize = when {
        fontScale >= 1.7f -> 8.5.sp
        fontScale >= 1.3f -> 9.sp
        else -> 10.sp
    }

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            // Gregorian Day Number
            Text(
                text = day.date.dayOfMonth.toString(),
                style = type.bodyMedium.copy(
                    fontSize = gregorianFontSize,
                    fontWeight = gregorianWeight,
                    fontFeatureSettings = "tnum",
                ),
                color = gregorianTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )

            // Hijri Day Number inside the selection
            if (hijriDayText.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (day.isHijriMonthStart && isCurrentMonth) {
                        // Subtle dot indicator for first day of Hijri month
                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(3.5.dp)
                                .background(
                                    colors.primary,
                                    CircleShape,
                                ),
                        )
                    }
                    Text(
                        text = hijriDayText,
                        style = type.labelSmall.copy(
                            fontSize = hijriFontSize,
                            fontWeight = if (day.isHijriMonthStart) FontWeight.SemiBold else FontWeight.Normal,
                            fontFeatureSettings = "tnum",
                        ),
                        color = hijriTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else {
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun SelectedDateScheduleSection(
    selectedDate: LocalDate,
    scheduleState: CalendarScheduleState,
    timeFormat: PrayerTimeFormat,
    zone: java.time.ZoneId,
    hasLocation: Boolean,
    onOpenLocation: () -> Unit,
    onRetry: () -> Unit,
    locale: Locale,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val fullDateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
    }
    val fullDateHeading = remember(selectedDate, fullDateFormatter) {
        fullDateFormatter.format(selectedDate)
    }
    val hijriDate = remember(selectedDate) {
        CalendarDateConverter.toHijrahDate(selectedDate)
    }
    val fullHijriFormatted = remember(hijriDate, locale) {
        CalendarDateConverter.formatHijriDateFull(hijriDate, locale)
    }

    val timeFormatter = remember(timeFormat, locale) {
        DateTimeFormatter.ofPattern(
            if (timeFormat == PrayerTimeFormat.TWELVE_HOUR) "h:mm a" else "HH:mm",
            locale,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Selected Date Header (Restrained, no technical provider/cache badges)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = fullDateHeading,
                style = type.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                ),
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            if (fullHijriFormatted.isNotEmpty()) {
                Text(
                    text = fullHijriFormatted,
                    style = type.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // Schedule Content Card based on State
        when {
            !hasLocation || scheduleState is CalendarScheduleState.NoLocation -> {
                CalendarNoLocationCard(onOpenLocation)
            }
            scheduleState is CalendarScheduleState.UnsupportedRange -> {
                CalendarUnsupportedRangeCard()
            }
            scheduleState is CalendarScheduleState.Error -> {
                CalendarErrorCard(onRetry)
            }
            scheduleState is CalendarScheduleState.Success -> {
                CalendarScheduleCard(
                    schedule = scheduleState.schedule,
                    formatter = timeFormatter,
                    zone = zone,
                )
            }
            scheduleState is CalendarScheduleState.Loading -> {
                CalendarLoadingCard()
            }
        }
    }
}

@Composable
private fun CalendarScheduleCard(
    schedule: DailyPrayerSchedule,
    formatter: DateTimeFormatter,
    zone: java.time.ZoneId,
) {
    val times = remember(schedule, formatter, zone) {
        schedule.timings.associate { it.name to formatter.format(it.instant.atZone(zone)) }
    }

    SalahCard(
        modifier = Modifier.testTag("calendar-prayer-schedule"),
        shape = RoundedCornerShape(20.dp),
        inset = 0.dp,
        gap = 0.dp,
    ) {
        Column {
            PrayerName.entries.forEachIndexed { index, prayer ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    )
                }
                val timeString = times[prayer] ?: "—"
                CalendarPrayerTimeRow(
                    prayer = prayer,
                    time = timeString,
                )
            }
        }
    }
}

@Composable
private fun CalendarPrayerTimeRow(
    prayer: PrayerName,
    time: String,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val isSunrise = prayer == PrayerName.SUNRISE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 18.dp)
            .testTag("calendar-prayer-${prayer.name.lowercase(Locale.ROOT)}")
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    SalahIcons.prayer(prayer),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSunrise) colors.onSurfaceVariant.copy(alpha = 0.55f) else colors.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(prayer.label()),
                color = if (isSunrise) colors.onSurfaceVariant.copy(alpha = 0.65f) else colors.onSurface,
                style = type.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
            )
        }

        Text(
            text = time,
            color = if (isSunrise) colors.onSurfaceVariant.copy(alpha = 0.65f) else colors.onSurface,
            style = type.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
                letterSpacing = 0.sp,
            ),
        )
    }
}

@Composable
private fun CalendarLoadingCard() {
    SalahCard(shape = RoundedCornerShape(20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.loading_times),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarErrorCard(onRetry: () -> Unit) {
    SalahCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.times_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun CalendarUnsupportedRangeCard() {
    SalahCard(shape = RoundedCornerShape(20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.unsupported_date_range),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarNoLocationCard(onOpenLocation: () -> Unit) {
    SalahCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.calendar_no_location_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.calendar_no_location_body),
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
