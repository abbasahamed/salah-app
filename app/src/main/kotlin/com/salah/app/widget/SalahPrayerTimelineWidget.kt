package com.salah.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.salah.app.MainActivity

class SalahPrayerTimelineWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WidgetDataRepository(context)
        val snapshot = repository.loadSnapshot()

        provideContent {
            TimelineWidgetContent(snapshot)
        }
    }
}

@Composable
private fun TimelineWidgetContent(snapshot: WidgetSnapshot) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(22.dp)
            .background(SalahWidgetColors.background)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!snapshot.hasLocation) {
            NoLocationTimelineContent()
        } else if (!snapshot.isAvailable || snapshot.nextPrayerName == null) {
            UnavailableTimelineContent(snapshot.localityName)
        } else {
            ActiveTimelineContent(snapshot)
        }
    }
}

@Composable
private fun ActiveTimelineContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val fontScale = context.resources.configuration.fontScale
    val isLargeFont = fontScale >= 1.3f
    val isExtraLargeFont = fontScale >= 1.5f

    val eyebrowFontSize = if (isLargeFont) 11.sp else 12.sp
    val countdownFontSize = when {
        isExtraLargeFont -> 26.sp
        isLargeFont -> 30.sp
        else -> 34.sp
    }
    val contextFontSize = if (isExtraLargeFont) 12.sp else 13.sp
    val cityFontSize = if (isLargeFont) 11.sp else 12.sp

    val countdownText = if (isExtraLargeFont) {
        snapshot.countdownCompactText ?: snapshot.countdownText.orEmpty()
    } else {
        snapshot.countdownText.orEmpty()
    }

    // Adaptive timeline items: prioritize 3 relevant prayers on 200% font scale
    val displayedTimeline = if (isExtraLargeFont) {
        selectRelevantPrayers(snapshot.timeline)
    } else {
        snapshot.timeline
    }

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        // Top section: Hero countdown + context line, with locality on top right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                // Eyebrow: sentence-case "Next prayer"
                Text(
                    text = "Next prayer",
                    style = TextStyle(
                        color = SalahWidgetColors.textSecondary,
                        fontSize = eyebrowFontSize,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )

                // Primary visual feature: Large countdown with refined presence
                Text(
                    text = countdownText,
                    style = TextStyle(
                        color = SalahWidgetColors.accent,
                        fontSize = countdownFontSize,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )

                // Subtitle context line: "Until Fajr · 05:32 tomorrow" or "Until Asr · 15:08"
                Text(
                    text = snapshot.heroContextLine,
                    style = TextStyle(
                        color = SalahWidgetColors.textPrimary,
                        fontSize = contextFontSize,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }

            // Top-right: Locality name (secondary, regular weight)
            if (!snapshot.localityName.isNullOrEmpty()) {
                Text(
                    text = snapshot.localityName,
                    style = TextStyle(
                        color = SalahWidgetColors.textSecondary,
                        fontSize = cityFontSize,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(GlanceModifier.defaultWeight())

        // Bottom section: Compact timeline of obligatory prayers
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            displayedTimeline.forEach { item ->
                TimelineColumnItem(
                    item = item,
                    modifier = GlanceModifier.defaultWeight(),
                    isExtraLargeFont = isExtraLargeFont,
                )
            }
        }
    }
}

private fun selectRelevantPrayers(items: List<WidgetTimelineItem>): List<WidgetTimelineItem> {
    if (items.size <= 3) return items
    val nextIdx = items.indexOfFirst { it.isNext }.coerceAtLeast(0)
    return when {
        nextIdx + 3 <= items.size -> items.subList(nextIdx, nextIdx + 3)
        else -> items.takeLast(3)
    }
}

@Composable
private fun TimelineColumnItem(
    item: WidgetTimelineItem,
    modifier: GlanceModifier = GlanceModifier,
    isExtraLargeFont: Boolean = false,
) {
    val nameColor = when {
        item.isNext -> SalahWidgetColors.accent
        item.isPassed -> SalahWidgetColors.textMuted
        else -> SalahWidgetColors.textSecondary
    }

    val timeColor = when {
        item.isNext -> SalahWidgetColors.accent
        item.isPassed -> SalahWidgetColors.textMuted
        else -> SalahWidgetColors.textPrimary
    }

    val nameWeight = if (item.isNext) FontWeight.Bold else FontWeight.Normal
    val timeWeight = if (item.isNext) FontWeight.Bold else FontWeight.Normal

    // Ensure explicit transparent background when not selected to prevent stale RemoteViews background retention
    val cellModifier = modifier
        .cornerRadius(8.dp)
        .background(
            if (item.isNext) SalahWidgetColors.accentContainer
            else SalahWidgetColors.transparent
        )
        .padding(vertical = 4.dp, horizontal = 2.dp)

    val itemFontSize = if (isExtraLargeFont) 13.sp else 12.sp

    Column(
        modifier = cellModifier,
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text = item.label,
            style = TextStyle(
                color = nameColor,
                fontSize = itemFontSize,
                fontWeight = nameWeight,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = item.timeFormatted,
            style = TextStyle(
                color = timeColor,
                fontSize = itemFontSize,
                fontWeight = timeWeight,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun NoLocationTimelineContent() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = "Salah",
            style = TextStyle(
                color = SalahWidgetColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Set your location",
            style = TextStyle(
                color = SalahWidgetColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = "Tap to choose a city and see your daily prayer timeline",
            style = TextStyle(
                color = SalahWidgetColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun UnavailableTimelineContent(locality: String?) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = locality ?: "Salah",
            style = TextStyle(
                color = SalahWidgetColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Prayer times unavailable",
            style = TextStyle(
                color = SalahWidgetColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = "Tap to refresh in Salah",
            style = TextStyle(
                color = SalahWidgetColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 2,
        )
    }
}
