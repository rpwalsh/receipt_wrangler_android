/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.walshtech.receiptwrangler.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1206),              // Dark brown text on gold buttons
    primaryContainer = Color(0xFF2A1E0F),       // Muted gold container
    onPrimaryContainer = Ink,
    secondary = InkMuted,                        // Muted cream for secondary text
    onSecondary = Color(0xFF1A1206),
    secondaryContainer = PanelAlt,
    onSecondaryContainer = Ink,
    background = Bg,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = PanelAlt,
    onSurfaceVariant = InkMuted,
    outline = Hairline,
    outlineVariant = Color(0xFF2A1E12),
    tertiary = Emerald,
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFEF6B62),
    onError = Color(0xFF2B0906),
    inverseSurface = Ink,
    inverseOnSurface = Bg
)

private val LightColors = lightColorScheme(
    primary = GoldDark,                          // Darker gold for light mode contrast
    onPrimary = Color(0xFFFFFBF5),               // Cream text on gold buttons
    primaryContainer = Color(0xFFF5E6C8),        // Light gold container
    onPrimaryContainer = Color(0xFF2A1A08),
    secondary = InkMutedLight,                   // Muted brown for secondary text
    onSecondary = Color(0xFFFFFBF5),
    secondaryContainer = Color(0xFFF0E4D0),
    onSecondaryContainer = Color(0xFF2A1A09),
    background = BgLight,
    onBackground = InkLight,
    surface = PanelLight,
    onSurface = InkLight,
    surfaceVariant = PanelAltLight,
    onSurfaceVariant = InkMutedLight,
    outline = HairlineLight,
    outlineVariant = Color(0xFFE8DCC8),
    tertiary = Emerald,
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    inverseSurface = InkLight,
    inverseOnSurface = BgLight
)

@Composable
fun ReceiptWranglerTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (useDark) DarkColors else LightColors
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
