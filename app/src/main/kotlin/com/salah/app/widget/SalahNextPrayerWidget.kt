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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.salah.app.MainActivity

class SalahNextPrayerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WidgetDataRepository(context)
        val snapshot = repository.loadSnapshot()

        provideContent {
            NextPrayerWidgetContent(snapshot)
        }
    }
}

@Composable
private fun NextPrayerWidgetContent(snapshot: WidgetSnapshot) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(22.dp)
            .background(SalahWidgetColors.background)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (!snapshot.hasLocation) {
            NoLocationNextPrayerContent()
        } else if (!snapshot.isAvailable || snapshot.nextPrayerName == null) {
            UnavailableNextPrayerContent(snapshot.localityName)
        } else {
            ActiveNextPrayerContent(snapshot)
        }
    }
}

@Composable
private fun ActiveNextPrayerContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val fontScale = context.resources.configuration.fontScale
    val isLargeFont = fontScale >= 1.3f
    val isExtraLargeFont = fontScale >= 1.7f

    val eyebrowFontSize = if (isExtraLargeFont) 11.sp else 12.sp
    val nameFontSize = if (isExtraLargeFont) 18.sp else 20.sp
    val timeFontSize = when {
        isExtraLargeFont -> 32.sp
        isLargeFont -> 38.sp
        else -> 42.sp
    }
    val countdownFontSize = if (isExtraLargeFont) 13.sp else 14.sp
    val countdownDisplay = if (isLargeFont) {
        snapshot.countdownCompactText ?: snapshot.countdownText.orEmpty()
    } else {
        snapshot.countdownText.orEmpty()
    }

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.Bottom,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Spacer(GlanceModifier.defaultWeight())

        // Eyebrow: sentence-case "Next prayer" with contextual tomorrow indicator
        val eyebrow = if (snapshot.isTomorrow) "Next prayer · Tomorrow" else "Next prayer"
        Text(
            text = eyebrow,
            style = TextStyle(
                color = SalahWidgetColors.textSecondary,
                fontSize = eyebrowFontSize,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
        )

        Spacer(GlanceModifier.height(2.dp))

        // Prayer name: "Dhuhr"
        Text(
            text = snapshot.nextPrayerName.orEmpty(),
            style = TextStyle(
                color = SalahWidgetColors.textPrimary,
                fontSize = nameFontSize,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        // Large time digits around 40-42sp with regular/medium weight (adaptively scaled on large fonts)
        Row(
            verticalAlignment = Alignment.Vertical.Bottom,
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Text(
                text = snapshot.nextPrayerTimeFormatted.orEmpty(),
                style = TextStyle(
                    color = SalahWidgetColors.textPrimary,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
            if (!snapshot.nextPrayerPeriod.isNullOrEmpty()) {
                Text(
                    text = " ${snapshot.nextPrayerPeriod}",
                    style = TextStyle(
                        color = SalahWidgetColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(GlanceModifier.height(2.dp))

        // Remaining countdown label: adaptive duration formatting
        Text(
            text = countdownDisplay,
            style = TextStyle(
                color = SalahWidgetColors.accent,
                fontSize = countdownFontSize,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun NoLocationNextPrayerContent() {
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
            text = "Tap to choose a city",
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
private fun UnavailableNextPrayerContent(locality: String?) {
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 2,
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
