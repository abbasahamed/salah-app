package com.salah.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object SalahWidgetColors {
    // Surfaces matching Editorial Luxury Obsidian / Slate in dark, Warm Porcelain in light
    val background = ColorProvider(day = Color(0xFFF5F3EF), night = Color(0xFF16171D))
    val surfaceContainer = ColorProvider(day = Color(0xFFEBE6DC), night = Color(0xFF1C1D22))
    val border = ColorProvider(day = Color(0x241A1A1E), night = Color(0x1FFFFFFF))
    val transparent = ColorProvider(day = Color.Transparent, night = Color.Transparent)

    // Typography tokens
    val textPrimary = ColorProvider(day = Color(0xFF1A1A1E), night = Color(0xFFF5F5F7))
    val textSecondary = ColorProvider(day = Color(0xFF6B6E78), night = Color(0xFF999BA3))
    val textMuted = ColorProvider(day = Color(0xFFA0A2A8), night = Color(0xFF5A5D66))

    // Accents: Warm golden bronze in light mode, Champagne gold in dark mode
    val accent = ColorProvider(day = Color(0xFF875F1A), night = Color(0xFFD2AE70))
    val accentContainer = ColorProvider(day = Color(0xFFEFE7D8), night = Color(0xFF2B2314))
    val progressBackground = ColorProvider(day = Color(0x141A1A1E), night = Color(0x1FFFFFFF))
}
