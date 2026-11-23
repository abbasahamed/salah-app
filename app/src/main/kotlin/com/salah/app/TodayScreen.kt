package com.salah.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.salah.designsystem.SalahCard
import com.salah.designsystem.SectionHeading
import com.salah.location.LocationMode
import com.salah.location.SalahLocation
import com.salah.prayer.DailyPrayerSchedule
import com.salah.prayer.NextPrayer
import com.salah.prayer.PrayerName
import com.salah.prayer.PrayerScheduleResult
import com.salah.prayer.PrayerTime
import com.salah.prayer.PrayerTimeFormat
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class HeroSurfaceVariant {
    TONAL_CARD,
    CARDLESS,
}

@Composable
internal fun TodayScreen(
    state: TodayState,
    selection: LocationSelection,
    openLocation: () -> Unit,
    useCurrent: () -> Unit,
    retry: () -> Unit,
    heroSurfaceVariant: HeroSurfaceVariant = HeroSurfaceVariant.CARDLESS,
) {
    val location = selection.value
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    val format = state.settings.format
    val timeFormat = remember(format, locale) {
        DateTimeFormatter.ofPattern(if (format == PrayerTimeFormat.TWELVE_HOUR) "h:mm a" else "HH:mm", locale)
    }
    val periodFormat = remember(locale) { DateTimeFormatter.ofPattern("a", locale) }
    val source = state.daily as? PrayerScheduleResult.Available
    val schedule = source?.schedule
    val next = state.next
    val missing = stringResource(if (state.daily == null) R.string.loading_entry else R.string.unavailable)
    val displayDate = (state.daily as? PrayerScheduleResult.Available)?.schedule?.request?.date
        ?: state.date
        ?: location?.zone?.let { LocalDate.now(it) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        val fontScale = density.fontScale
        // Compact Editorial Luxury layout:
        // Location row (~40dp) + Hero (~140dp) + Heading (~28dp) + Schedule (~280dp) = ~488dp.
        // On standard phones (including Galaxy S23, 390–412dp wide, ~700dp viewport height above nav),
        // all six prayers fit comfortably in a single screen without scrolling at standard font scale.
        val minRequiredHeight = 490.dp * fontScale.coerceAtLeast(1f)
        val fitsInSingleScreen = maxHeight >= minRequiredHeight && fontScale <= 1.15f

        if (fitsInSingleScreen) {
            // NON-SCROLLING ONE-SCREEN VIEWPORT FOR REFERENCE & STANDARD PHONES
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
            ) {
                if (!selection.ready) {
                    Text(
                        stringResource(R.string.loading_location),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                } else if (location == null) {
                    TodayLocationHeader(
                        location = null,
                        date = null,
                        openLocation = openLocation,
                    )
                    Spacer(Modifier.height(10.dp))
                    LocationOnboarding(useCurrent, openLocation)
                } else if (next != null) {
                    TodayLocationHeader(
                        location = location,
                        date = displayDate,
                        openLocation = openLocation,
                    )
                    Spacer(Modifier.height(10.dp))
                    EditorialNextPrayerHero(
                        state = state,
                        location = location,
                        next = next,
                        timeFormat = timeFormat,
                        periodFormat = periodFormat,
                        dateFormat = dateFormat,
                        format = format,
                        openLocation = openLocation,
                    )
                    Spacer(Modifier.height(10.dp))
                    ScheduleHeading()
                    Spacer(Modifier.height(6.dp))
                    PrayerScheduleCard(schedule, timeFormat, missing, next.time, state.preceding)
                } else {
                    TodayLocationHeader(
                        location = location,
                        date = displayDate,
                        openLocation = openLocation,
                    )
                    Spacer(Modifier.height(10.dp))
                    UnavailableNextPrayerCard(state, retry)
                }
            }
        } else {
            // ACCESSIBLE SCROLLABLE CONTAINER FOR LARGE FONT SCALING (130%, 200%) OR COMPACT DISPLAYS
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!selection.ready) {
                    Text(
                        stringResource(R.string.loading_location),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else if (location == null) {
                    TodayLocationHeader(
                        location = null,
                        date = null,
                        openLocation = openLocation,
                    )
                    LocationOnboarding(useCurrent, openLocation)
                } else if (next != null) {
                    TodayLocationHeader(
                        location = location,
                        date = displayDate,
                        openLocation = openLocation,
                    )
                    EditorialNextPrayerHero(
                        state = state,
                        location = location,
                        next = next,
                        timeFormat = timeFormat,
                        periodFormat = periodFormat,
                        dateFormat = dateFormat,
                        format = format,
                        openLocation = openLocation,
                    )
                    ScheduleHeading()
                    PrayerScheduleCard(schedule, timeFormat, missing, next.time, state.preceding)
                } else {
                    TodayLocationHeader(
                        location = location,
                        date = displayDate,
                        openLocation = openLocation,
                    )
                    UnavailableNextPrayerCard(state, retry)
                }
                Spacer(Modifier.height(112.dp))
            }
        }
    }
}

private fun Duration.toFractionalSeconds() = seconds + nano / 1_000_000_000.0

@Composable
private fun EditorialNextPrayerHero(
    state: TodayState,
    location: SalahLocation,
    next: NextPrayer,
    timeFormat: DateTimeFormatter,
    periodFormat: DateTimeFormatter,
    dateFormat: DateTimeFormatter,
    format: PrayerTimeFormat,
    openLocation: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val local = next.time.instant.atZone(location.zone)
    val interval = state.preceding?.let { Duration.between(it.instant, next.time.instant) }
    val progress = interval?.takeIf { !it.isZero && !it.isNegative && next.remaining <= it }?.let {
        (1 - next.remaining.toFractionalSeconds() / it.toFractionalSeconds()).toFloat().coerceIn(0f, 1f)
    }
    val isTomorrow = local.toLocalDate() != state.date
    val dateLabel = if (isTomorrow) {
        if (local.toLocalDate() == state.date?.plusDays(1)) stringResource(R.string.tomorrow)
        else local.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    } else null

    val seconds = next.remaining.seconds + if (next.remaining.nano > 0) 1 else 0
    val hours = (seconds / 3600).toInt()
    val minutes = (seconds / 60 % 60).toInt()
    val secondsPart = (seconds % 60).toInt()
    val title = stringResource(R.string.next_prayer)
    val prayerName = stringResource(next.time.name.label())
    val spokenHours = pluralStringResource(R.plurals.spoken_hours, hours, hours)
    val spokenMinutes = pluralStringResource(R.plurals.spoken_minutes, minutes, minutes)
    val spokenSeconds = pluralStringResource(R.plurals.spoken_seconds, secondsPart, secondsPart)
    val spokenCountdown = stringResource(R.string.spoken_remaining, spokenHours, spokenMinutes, spokenSeconds)
    val timeFormatted = local.format(timeFormat)
    val period = if (format == PrayerTimeFormat.TWELVE_HOUR) local.format(periodFormat) else null
    val timeDigits = if (period != null) timeFormatted.removeSuffix(period).trim() else timeFormatted
    val context = if (dateLabel != null) stringResource(R.string.dial_date_time, dateLabel, timeFormatted) else timeFormatted
    val description = stringResource(R.string.dial_description, title, prayerName, context, spokenCountdown)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 1. Eyebrow: concise "Next prayer" with date pill when tomorrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = type.labelMedium.copy(
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            if (dateLabel != null) {
                Text(
                    text = dateLabel,
                    style = type.labelMedium.copy(
                        fontSize = 13.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                )
            }
        }

        // 2. Hero Readout with semantics
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = description
                    if (progress != null) progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Bold prayer name: "Dhuhr"
            Text(
                text = prayerName,
                style = type.headlineMedium.copy(
                    fontSize = 33.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = colors.onSurface,
            )

        // 3. Large focal start time: "11:47"
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = timeDigits,
                style = type.displayLarge.copy(
                    fontSize = 56.sp,
                    lineHeight = 60.sp,
                    letterSpacing = (-1.2).sp,
                    fontWeight = FontWeight(300),
                    fontFeatureSettings = "tnum",
                ),
                color = colors.onSurface,
            )
            if (period != null) {
                Text(
                    text = period,
                    style = type.titleLarge.copy(
                        fontSize = 17.sp,
                        color = colors.onSurfaceVariant,
                    ),
                    modifier = Modifier.padding(bottom = 10.dp, start = 6.dp),
                )
            }
        }

        // 4. Calm remaining countdown: "in 5 hr 20 min"
        val countdownText = buildString {
            append("in ")
            if (hours > 0) {
                append("$hours hr ")
            }
            append("$minutes min")
            if (hours == 0 && secondsPart > 0) {
                append(" $secondsPart s")
            }
        }
        Text(
            text = countdownText,
            style = type.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.primary,
        )

        // 5. Subtle quiet horizontal interval progress bar
        if (progress != null) {
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(colors.onSurface.copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0.01f, 1f))
                        .height(3.dp)
                        .background(colors.primary),
                )
            }
        }
        }
    }
}

@Composable
private fun ScheduleHeading() {
    Text(
        text = stringResource(R.string.today_prayer_times),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun PrayerScheduleCard(
    schedule: DailyPrayerSchedule?,
    formatter: DateTimeFormatter,
    missing: String,
    upcoming: PrayerTime?,
    preceding: PrayerTime?,
) {
    val times = remember(schedule, formatter) {
        schedule?.timings?.associate { it.name to formatter.format(it.instant.atZone(schedule.request.location.zone)) }
    }
    SalahCard(
        modifier = Modifier.testTag("prayer-schedule"),
        shape = RoundedCornerShape(20.dp),
        inset = 0.dp,
        gap = 0.dp,
    ) {
        Column {
            PrayerName.entries.forEachIndexed { index, prayer ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    )
                }
                val occurrence = schedule?.timings?.firstOrNull { it.name == prayer }
                val isUpcomingOccurrence = occurrence != null && upcoming != null &&
                    occurrence.name == upcoming.name && occurrence.instant == upcoming.instant
                val isPrecedingOccurrence = occurrence != null && preceding != null &&
                    occurrence.name == preceding.name && occurrence.instant == preceding.instant
                val status = when {
                    isUpcomingOccurrence -> R.string.upcoming
                    isPrecedingOccurrence -> R.string.preceding_prayer
                    else -> null
                }
                PrayerTimeRow(prayer, times?.get(prayer) ?: missing, status)
            }
        }
    }
}

@Composable
private fun PrayerTimeRow(prayer: PrayerName, time: String, status: Int?) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val isNext = status == R.string.upcoming
    val isSunrise = prayer == PrayerName.SUNRISE
    val rowTone = if (isNext) colors.primary.copy(alpha = 0.05f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(rowTone)
            .padding(horizontal = 16.dp)
            .testTag("prayer-${prayer.name.lowercase(Locale.ROOT)}")
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Icon(
                    SalahIcons.prayer(prayer),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when {
                        isNext -> colors.primary
                        isSunrise -> colors.onSurfaceVariant.copy(alpha = 0.55f)
                        else -> colors.onSurfaceVariant
                    },
                )
            }
            Text(
                text = stringResource(prayer.label()),
                color = when {
                    isNext -> colors.primary
                    isSunrise -> colors.onSurfaceVariant.copy(alpha = 0.65f)
                    else -> colors.onSurface
                },
                style = type.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
            )
        }

        Text(
            text = time,
            color = when {
                isNext -> colors.primary
                isSunrise -> colors.onSurfaceVariant.copy(alpha = 0.65f)
                else -> colors.onSurface
            },
            style = type.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Medium,
                fontFeatureSettings = "tnum",
                letterSpacing = 0.sp,
            ),
        )
    }
}

@Composable
private fun UnavailableNextPrayerCard(state: TodayState, retry: () -> Unit) {
    SalahCard(emphasized = true) {
        val unavailable = state.daily as? PrayerScheduleResult.Unavailable
        val message = when {
            state.daily == null -> R.string.loading_times
            unavailable != null -> R.string.times_error
            state.nextLoading -> R.string.loading_next_prayer
            else -> R.string.next_unavailable
        }
        if (state.daily == null || state.nextLoading) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 90.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(stringResource(message), style = MaterialTheme.typography.titleMedium)
        }
        val source = state.daily as? PrayerScheduleResult.Available
        if (unavailable?.retryable == true || (source != null && !state.nextLoading)) {
            FilledTonalButton(onClick = retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
internal fun TodayLocationHeader(
    location: SalahLocation?,
    date: LocalDate?,
    openLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val datePattern = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }
    val dateText = remember(date, datePattern) { date?.format(datePattern) }

    val isAutomatic = location?.mode == LocationMode.AUTOMATIC
    val icon = when {
        location == null -> SalahIcons.Place
        isAutomatic -> SalahIcons.NearMe
        else -> SalahIcons.Place
    }
    val cityName = location?.names?.displayName ?: stringResource(R.string.set_location)
    val accessibilityText = if (location != null) {
        val modeDesc = if (isAutomatic) "Current location" else "Selected city"
        "$modeDesc: $cityName${dateText?.let { ", $it" } ?: ""}. Tap to change location."
    } else {
        "No location selected. Tap to set location."
    }

    if (fontScale > 1.25f) {
        // High font scaling (130%, 200%): wrap date below city
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = openLocation,
                    )
                    .testTag("today-location")
                    .semantics(mergeDescendants = true) {
                        contentDescription = accessibilityText
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = SalahIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            if (dateText != null) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                    ),
                    modifier = Modifier.padding(start = 22.dp),
                )
            }
        }
    } else {
        // Standard slim single row
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 48.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = openLocation,
                    )
                    .testTag("today-location")
                    .semantics(mergeDescendants = true) {
                        contentDescription = accessibilityText
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = SalahIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            if (dateText != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
