package com.salah.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salah.designsystem.SalahCard
import java.time.Duration
import kotlin.math.cos
import kotlin.math.sin

private const val ArcStart = 156f
private const val ArcSweep = 228f

internal data class DialReadout(
    val prayer: String, val time: String, val date: String?, val period: String?,
)

@Composable
private fun DialReadout.styledTime(): AnnotatedString {
    val typography = MaterialTheme.typography
    val timeWithoutPeriod = period?.let(time::removeSuffix)
    return buildAnnotatedString {
        if (timeWithoutPeriod == null || timeWithoutPeriod == time) {
            append(time)
        } else {
            append(timeWithoutPeriod)
            withStyle(typography.headlineSmall.toSpanStyle()) { append(period.orEmpty()) }
        }
    }
}

@Composable
internal fun SalahDial(readout: DialReadout, remaining: Duration, progress: Float?) {
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val seconds = remaining.seconds + if (remaining.nano > 0) 1 else 0
    val countdown = stringResource(R.string.countdown, seconds / 3600, seconds / 60 % 60, seconds % 60)
    val title = stringResource(R.string.next_prayer)
    val hours = (seconds / 3600).toInt()
    val minutes = (seconds / 60 % 60).toInt()
    val secondsPart = (seconds % 60).toInt()
    val spokenHours = pluralStringResource(R.plurals.spoken_hours, hours, hours)
    val spokenMinutes = pluralStringResource(R.plurals.spoken_minutes, minutes, minutes)
    val spokenSeconds = pluralStringResource(R.plurals.spoken_seconds, secondsPart, secondsPart)
    val spokenCountdown = stringResource(R.string.spoken_remaining, spokenHours, spokenMinutes, spokenSeconds)
    val context = if (readout.date != null) stringResource(R.string.dial_date_time, readout.date, readout.time) else readout.time
    val description = stringResource(R.string.dial_description, title, readout.prayer, context, spokenCountdown)
    val me = rememberTextMeasurer()
    val styledTime = readout.styledTime()

    SalahCard(emphasized = true, inset = 16.dp, gap = 0.dp) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().clearAndSetSemantics {
                contentDescription = description
                if (progress != null) progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        ) {
            val diameter = maxWidth.coerceAtMost(236.dp)
            val aperture = (diameter - 52.dp).coerceAtLeast(0.dp)
            val density = LocalDensity.current
            val fits = remember(readout, styledTime, diameter, density, me, style) {
                density.fontScale <= 1.3f &&
                    with(density) {
                        me.measure(styledTime, style.displayMedium, maxLines = 1).size.width <= aperture.toPx() &&
                            me.measure(readout.prayer, style.headlineSmall, maxLines = 1).size.width <= aperture.toPx()
                    }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = style.labelMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                if (progress != null && fits) {
                    Box(
                        Modifier.size(diameter, diameter * 0.78f),
                        contentAlignment = Alignment.Center,
                    ) {
                        PrayerArch(progress, Modifier.matchParentSize())
                        PrayerReadout(readout, countdown, Modifier.offset(y = 14.dp))
                    }
                } else {
                    PrayerReadout(readout, countdown)
                    if (progress != null) {
                        Spacer(Modifier.height(12.dp))
                        PrayerArch(progress, Modifier.size(diameter.coerceAtMost(128.dp), 98.dp))
                    }
                }
                if (readout.date != null) {
                    Spacer(Modifier.height(9.dp))
                    if (fits) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
                            DialDate(readout.date, Modifier.weight(3f, fill = false))
                            HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
                        }
                    } else {
                        DialDate(readout.date)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DialDate(date: String, modifier: Modifier = Modifier) {
    Text(date, modifier, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun PrayerReadout(readout: DialReadout, countdown: String, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Crossfade(readout, animationSpec = tween(140), label = "Prayer boundary") { current ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.prayer, style = type.headlineSmall, textAlign = TextAlign.Center)
                Text(current.styledTime(), style = type.displayMedium, textAlign = TextAlign.Center)
            }
        }
        val colors = MaterialTheme.colorScheme
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.primaryContainer,
            contentColor = colors.primary,
            border = BorderStroke(1.dp, lerp(colors.primaryContainer, colors.primary, 0.22f)),
        ) {
            Text(
                countdown,
                Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = type.labelLarge.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrayerArch(progress: Float, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val guides = remember { List(13) { radialVector(ArcStart + ArcSweep * it / 12) } }
    val fraction by rememberUpdatedState(progress)
    Spacer(modifier.drawWithCache {
        val radius = (size.width / 2f - 15.dp.toPx()).coerceAtLeast(0f)
        val rise = radius * 0.94f
        val axes = Offset(radius, rise)
        val center = Offset(size.width / 2f, rise + 15.dp.toPx())
        val topLeft = center - axes
        val bounds = Size(radius * 2, rise * 2)
        val bed = lerp(colors.surfaceVariant, colors.surfaceContainerHighest, 0.91f)
        val lip = lerp(colors.surfaceVariant, colors.surfaceContainerHighest, 0.45f)
        val trackStroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        val activeStroke = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round)
        val outerStroke = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
        val grooveStroke = Stroke(5.5.dp.toPx(), cap = StrokeCap.Round)
        val tickWidth = 1.dp.toPx()
        val guidesInPixels = guides.mapIndexed { index, direction ->
            val major = index % 6 == 0
            val anchor = center + Offset(direction.x * axes.x, direction.y * axes.y)
            val start = anchor + direction * 7.dp.toPx()
            val length = when {
                major -> 12.dp
                index % 3 == 0 -> 10.5.dp
                else -> 9.dp
            }
            val end = anchor + direction * length.toPx()
            Triple(start, end, major)
        }
        onDrawBehind {
            if (radius <= 0f) return@onDrawBehind
            drawArc(lip, ArcStart, ArcSweep, false, topLeft, bounds, style = outerStroke)
            drawArc(colors.surfaceVariant, ArcStart, ArcSweep, false, topLeft, bounds, style = grooveStroke)
            drawArc(bed, ArcStart, ArcSweep, false, topLeft, bounds, style = trackStroke)
            guidesInPixels.forEach { (start, end, major) ->
                drawLine(
                    if (major) colors.onSurfaceVariant.copy(alpha = 0.72f) else bed,
                    start, end, strokeWidth = tickWidth, cap = StrokeCap.Round,
                )
            }
            if (fraction > 0f) {
                val sweep = ArcSweep * fraction
                val endpoint = radialVector(ArcStart + sweep)
                val mark = center + Offset(endpoint.x * axes.x, endpoint.y * axes.y)
                drawArc(colors.primary, ArcStart, sweep, false, topLeft, bounds, style = activeStroke)
                drawCircle(colors.primary, 4.2.dp.toPx(), mark)
                drawCircle(colors.surfaceVariant, 1.6.dp.toPx(), mark)
            }
        }
    })
}

private fun radialVector(angle: Float): Offset {
    val radians = Math.toRadians(angle.toDouble())
    return Offset(cos(radians).toFloat(), sin(radians).toFloat())
}
