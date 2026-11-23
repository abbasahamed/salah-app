package com.salah.app.qibla

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salah.app.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Premium native Qibla Compass Dial — Milestone 6.3 Final Micro-Polish.
 *
 * Precision instrument characteristics:
 * - Upright Kaaba glyph counter-rotated relative to dial so it remains strictly upright on screen.
 * - Champagne Gold strictly reserved for Qibla direction, Kaaba marker, and confirmed alignment.
 * - "Calibrating" rendered in neutral secondary gray (not gold).
 * - Simplified dial geometry with redundant middle concentric ring removed.
 * - Solid masking disc behind Kaaba marker with 1dp gold outline and clean radial separation.
 * - Useful central readout in Geist typography with throttled accessibility semantics.
 */
@Composable
fun QiblaCompass(
    headingDegrees: Float,
    qiblaBearingDegrees: Float,
    isFacingQibla: Boolean,
    isSensorActive: Boolean,
    modifier: Modifier = Modifier,
    relativeAngleDegrees: Float = 0f,
    isLowAccuracy: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // Continuous shortest-path angle rotation avoiding 360° jump-backs
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var lastTargetHeading by remember { mutableFloatStateOf(0f) }

    val desiredDialHeading = if (isSensorActive) headingDegrees else 0f
    val headingDelta = ((desiredDialHeading - lastTargetHeading + 540f) % 360f) - 180f
    targetRotation += headingDelta
    lastTargetHeading = desiredDialHeading

    val animatedDialRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.90f),
        label = "DialRotation",
    )

    val goldColor by animateColorAsState(
        if (isFacingQibla) colors.primary else colors.primary.copy(alpha = 0.85f),
        tween(200),
        label = "GoldColor",
    )

    val notchColor by animateColorAsState(
        if (isFacingQibla) colors.primary else colors.onSurface.copy(alpha = 0.28f),
        tween(200),
        label = "NotchColor",
    )

    val haloAlpha by animateFloatAsState(
        if (isFacingQibla) 0.22f else 0.0f,
        tween(250),
        label = "HaloAlpha",
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = minOf(size.width, size.height)
            // Outer tick circle radius leaving margin for lubber notch
            val outerRadius = (minDim / 2f) - with(density) { 18.dp.toPx() }
            val cardinalRadius = outerRadius - with(density) { 34.dp.toPx() }
            val hubRadius = with(density) { 66.dp.toPx() }

            // 1. Fixed Lubber Line Notch at top (12 o'clock)
            drawLubberNotch(center, outerRadius, notchColor)

            // 2. Faint inner-circle tonal fill behind center readout
            drawCircle(
                color = colors.onSurface.copy(alpha = 0.02f),
                radius = hubRadius,
                center = center,
            )
            drawCircle(
                color = colors.onSurface.copy(alpha = 0.06f),
                radius = hubRadius,
                center = center,
                style = Stroke(width = with(density) { 1.dp.toPx() }),
            )

            // 3. Rotating Compass Dial Track & Ticks
            rotate(-animatedDialRotation, pivot = center) {
                // Outer dial tick circle (redundant middle concentric ring removed per Milestone 6.3)
                drawCircle(
                    color = colors.onSurface.copy(alpha = 0.12f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = with(density) { 1.5.dp.toPx() }),
                )

                // 36 Dial tick marks (neutral tones - NO gold for ticks)
                for (i in 0 until 36) {
                    val angleDeg = i * 10f
                    val angleRad = Math.toRadians((angleDeg - 90f).toDouble())
                    val isCardinal = i % 9 == 0
                    val isMajor = i % 3 == 0

                    val tickLength = with(density) {
                        when {
                            isCardinal -> 12.dp.toPx()
                            isMajor -> 8.dp.toPx()
                            else -> 4.5.dp.toPx()
                        }
                    }
                    val strokeWidth = with(density) {
                        when {
                            isCardinal -> 2.2.dp.toPx()
                            isMajor -> 1.8.dp.toPx()
                            else -> 1.0.dp.toPx()
                        }
                    }
                    val tickAlpha = when {
                        isCardinal -> 0.65f
                        isMajor -> 0.38f
                        else -> 0.16f
                    }

                    val startX = center.x + (outerRadius - tickLength) * cos(angleRad).toFloat()
                    val startY = center.y + (outerRadius - tickLength) * sin(angleRad).toFloat()
                    val endX = center.x + outerRadius * cos(angleRad).toFloat()
                    val endY = center.y + outerRadius * sin(angleRad).toFloat()

                    drawLine(
                        color = colors.onSurface.copy(alpha = tickAlpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }

                // Qibla Pointer & Upright Kaaba marker
                drawQiblaIndicator(
                    center = center,
                    radius = outerRadius,
                    qiblaBearing = qiblaBearingDegrees,
                    currentRotation = animatedDialRotation,
                    goldColor = goldColor,
                    haloAlpha = haloAlpha,
                    density = density,
                    colors = colors,
                    isFacingQibla = isFacingQibla,
                    isLowAccuracy = isLowAccuracy,
                    hubRadius = hubRadius,
                )
            }

            // 4. Upright Cardinal Labels (N, E, S, W)
            // Rendered independently from rotating canvas so letters remain strictly upright relative to screen
            drawUprightCardinalLabels(
                center = center,
                radius = cardinalRadius,
                currentRotation = animatedDialRotation,
                colors = colors,
                density = density,
            )
        }

        // 5. Central Readout: Useful live heading or geographic bearing with clear relative guidance
        QiblaCentralReadout(
            headingDegrees = headingDegrees,
            qiblaBearingDegrees = qiblaBearingDegrees,
            relativeAngleDegrees = relativeAngleDegrees,
            isFacingQibla = isFacingQibla,
            isSensorActive = isSensorActive,
            isLowAccuracy = isLowAccuracy,
        )
    }
}

/**
 * Useful Central Readout:
 * - Live True North device heading (~44sp Geist Light, tabular numerals).
 * - Centered relative guidance below (~19sp Geist Medium): "18° right", "12° left", or "Facing Qibla".
 * - When in No-Sensor mode: shows geographic Qibla bearing clearly labeled "from north".
 * - When Low Accuracy: shows heading with "Calibrating" in neutral secondary gray (NOT gold).
 * - Clear semantic contentDescription to prevent rapid 60Hz speech flooding in TalkBack.
 */
@Composable
private fun QiblaCentralReadout(
    headingDegrees: Float,
    qiblaBearingDegrees: Float,
    relativeAngleDegrees: Float,
    isFacingQibla: Boolean,
    isSensorActive: Boolean,
    isLowAccuracy: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val roundedHeading = ((headingDegrees.roundToInt() % 360 + 360) % 360)
    val roundedBearing = ((qiblaBearingDegrees.roundToInt() % 360 + 360) % 360)
    val turnAngle = QiblaOrientationMath.turnAngleDegrees(relativeAngleDegrees)
    val turnDeg = abs(turnAngle.roundToInt())

    val semanticDescription = when {
        !isSensorActive -> stringResource(R.string.qibla_no_sensor_guidance, roundedBearing)
        isLowAccuracy -> stringResource(R.string.qibla_calibrating_title)
        isFacingQibla -> stringResource(R.string.qibla_facing)
        turnAngle > 0f -> stringResource(R.string.qibla_turn_right_short, turnDeg)
        else -> stringResource(R.string.qibla_turn_left_short, turnDeg)
    }

    Column(
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = semanticDescription
            }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            !isSensorActive -> {
                // Honest no-sensor state: show geographic bearing labeled "from north"
                Text(
                    text = "$roundedBearing°",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        letterSpacing = (-1.0).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.qibla_from_north_label),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            isLowAccuracy -> {
                // Low accuracy / needs calibration — neutral gray (NOT gold per Milestone 6.3)
                Text(
                    text = "$roundedHeading°",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        letterSpacing = (-1.0).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onBackground.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.qibla_calibrating_label),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            isFacingQibla -> {
                // Aligned state: Confirmed device heading & Champagne "Facing Qibla" with check
                Text(
                    text = "$roundedHeading°",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        letterSpacing = (-1.0).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = colors.primary,
                    )
                    Text(
                        text = stringResource(R.string.qibla_facing),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = colors.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                // Active turning guidance
                Text(
                    text = "$roundedHeading°",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        letterSpacing = (-1.0).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                val guidanceText = if (turnAngle > 0f) {
                    stringResource(R.string.qibla_turn_right_short, turnDeg)
                } else {
                    stringResource(R.string.qibla_turn_left_short, turnDeg)
                }
                Text(
                    text = guidanceText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun DrawScope.drawLubberNotch(
    center: Offset,
    outerRadius: Float,
    notchColor: Color,
) {
    val topY = center.y - outerRadius
    val notchHeight = 11f
    val path = Path().apply {
        moveTo(center.x - 6.5f, topY - notchHeight)
        lineTo(center.x + 6.5f, topY - notchHeight)
        lineTo(center.x, topY - 1.5f)
        close()
    }
    drawPath(path, color = notchColor)
}

/**
 * Draws cardinal direction letters (N, E, S, W) at their rotated orbital positions,
 * but with zero text rotation so the glyphs stay perfectly upright relative to the screen.
 */
private fun DrawScope.drawUprightCardinalLabels(
    center: Offset,
    radius: Float,
    currentRotation: Float,
    colors: androidx.compose.material3.ColorScheme,
    density: androidx.compose.ui.unit.Density,
) {
    // North is high-contrast neutral white/near-black (NOT gold!)
    val paintNorth = Paint().apply {
        color = colors.onSurface.toArgb()
        textSize = with(density) { 15.sp.toPx() }
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // East, South, West in muted neutral secondary gray
    val paintOther = Paint().apply {
        color = colors.onSurfaceVariant.copy(alpha = 0.55f).toArgb()
        textSize = with(density) { 13.sp.toPx() }
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    val cardinals = listOf(
        Pair("N", 0f),
        Pair("E", 90f),
        Pair("S", 180f),
        Pair("W", 270f),
    )

    cardinals.forEach { (text, angleDeg) ->
        val orbitalAngleDeg = angleDeg - currentRotation
        val angleRad = Math.toRadians((orbitalAngleDeg - 90f).toDouble())
        val x = center.x + radius * cos(angleRad).toFloat()
        val y = center.y + radius * sin(angleRad).toFloat()

        val paint = if (text == "N") paintNorth else paintOther
        val fontMetrics = paint.fontMetrics
        val textY = y - (fontMetrics.ascent + fontMetrics.descent) / 2f

        drawContext.canvas.nativeCanvas.drawText(text, x, textY, paint)
    }
}

/**
 * Draws the refined Qibla directional indicator:
 * - Thinner 1.8dp needle with subtle fade towards the central hub.
 * - Solid circular dark/light disc behind Kaaba marker that cleanly masks underlying tick lines.
 * - Subtle 1dp Champagne Gold outline around the disc.
 * - Upright Kaaba glyph counter-rotated relative to rotating dial so it always appears upright on screen.
 * - Outward-pointing arrow towards the dial rim.
 * - Aligned state halo and rim accent arc.
 */
private fun DrawScope.drawQiblaIndicator(
    center: Offset,
    radius: Float,
    qiblaBearing: Float,
    currentRotation: Float,
    goldColor: Color,
    haloAlpha: Float,
    density: androidx.compose.ui.unit.Density,
    colors: androidx.compose.material3.ColorScheme,
    isFacingQibla: Boolean,
    isLowAccuracy: Boolean,
    hubRadius: Float,
) {
    rotate(qiblaBearing, pivot = center) {
        val isDark = colors.background.red < 0.5f

        // Marker disc centered along the outer tick band
        val discRadius = with(density) { 13.5.dp.toPx() }
        val markerCenterY = center.y - radius + with(density) { 6.dp.toPx() }
        val markerCenter = Offset(center.x, markerCenterY)

        // 1. Subtle glowing halo behind Kaaba marker when aligned
        if (isFacingQibla && haloAlpha > 0f) {
            drawCircle(
                color = goldColor.copy(alpha = haloAlpha),
                radius = with(density) { 22.dp.toPx() },
                center = markerCenter,
            )
            // Aligned rim accent arc
            drawArc(
                color = goldColor.copy(alpha = 0.45f),
                startAngle = -104f,
                sweepAngle = 28f,
                useCenter = false,
                topLeft = Offset(center.x - radius - 2f, center.y - radius - 2f),
                size = Size((radius + 2f) * 2f, (radius + 2f) * 2f),
                style = Stroke(width = with(density) { 2.dp.toPx() }),
            )
        }

        // 2. Solid dark/light disc behind marker — cleanly masks underlying tick lines
        val discBgColor = if (isDark) colors.surfaceContainer else colors.surface
        drawCircle(
            color = discBgColor,
            radius = discRadius,
            center = markerCenter,
        )

        // 3. Subtle ~1dp champagne outline
        val outlineAlpha = if (isFacingQibla) 0.95f else 0.55f
        drawCircle(
            color = goldColor.copy(alpha = outlineAlpha),
            radius = discRadius,
            center = markerCenter,
            style = Stroke(width = with(density) { 1.dp.toPx() }),
        )

        // 4. Outward pointing Champagne arrow towards the dial rim
        val arrowHeight = with(density) { 4.5.dp.toPx() }
        val arrowHalfWidth = with(density) { 3.5.dp.toPx() }
        val arrowTipY = markerCenterY - discRadius - with(density) { 1.5.dp.toPx() }
        val arrowBaseY = markerCenterY - discRadius + with(density) { 2.dp.toPx() }
        val arrowPath = Path().apply {
            moveTo(center.x - arrowHalfWidth, arrowBaseY)
            lineTo(center.x + arrowHalfWidth, arrowBaseY)
            lineTo(center.x, arrowTipY)
            close()
        }
        drawPath(arrowPath, color = goldColor)

        // 5. Upright Kaaba Glyph inside the disc:
        // Counter-rotate by -(qiblaBearing - currentRotation) around markerCenter
        // so the Kaaba illustration ALWAYS remains upright on the device screen!
        val netRotation = qiblaBearing - currentRotation
        rotate(-netRotation, pivot = markerCenter) {
            val cubeWidth = with(density) { 15.dp.toPx() }
            val cubeHeight = with(density) { 15.dp.toPx() }
            val cubeLeft = markerCenter.x - (cubeWidth / 2f)
            val cubeTop = markerCenter.y - (cubeHeight / 2f)

            val kaabaBlack = if (isDark) Color(0xFF101114) else Color(0xFF23252A)
            drawRoundRect(
                color = kaabaBlack,
                topLeft = Offset(cubeLeft, cubeTop),
                size = Size(cubeWidth, cubeHeight),
                cornerRadius = CornerRadius(with(density) { 2.5.dp.toPx() }),
            )

            // Champagne Gold rim highlight around cube
            drawRoundRect(
                color = goldColor.copy(alpha = if (isFacingQibla) 0.85f else 0.45f),
                topLeft = Offset(cubeLeft, cubeTop),
                size = Size(cubeWidth, cubeHeight),
                cornerRadius = CornerRadius(with(density) { 2.5.dp.toPx() }),
                style = Stroke(width = with(density) { 1.dp.toPx() }),
            )

            // Champagne Gold Kiswah band across upper third
            val bandTop = cubeTop + (cubeHeight * 0.28f)
            val bandHeight = with(density) { 2.2.dp.toPx() }
            drawRect(
                color = goldColor,
                topLeft = Offset(cubeLeft, bandTop),
                size = Size(cubeWidth, bandHeight),
            )
        }

        // 6. Thinner 1.8dp needle with subtle fade towards center hub
        val needleStroke = with(density) { if (isFacingQibla) 2.0.dp.toPx() else 1.6.dp.toPx() }
        val needleAlpha = when {
            isLowAccuracy -> 0.22f
            isFacingQibla -> 0.85f
            else -> 0.48f
        }
        val pathEffect = if (isLowAccuracy) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null

        val needleTop = markerCenterY + discRadius - with(density) { 1.5.dp.toPx() }
        val needleBottom = center.y - hubRadius

        val needleBrush = Brush.verticalGradient(
            colors = listOf(
                goldColor.copy(alpha = needleAlpha),
                goldColor.copy(alpha = 0.08f),
            ),
            startY = needleTop,
            endY = needleBottom,
        )

        drawLine(
            brush = needleBrush,
            start = Offset(center.x, needleBottom),
            end = Offset(center.x, needleTop),
            strokeWidth = needleStroke,
            cap = StrokeCap.Round,
            pathEffect = pathEffect,
        )
    }
}
