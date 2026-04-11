/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Material Design 3 compliant spacing system.
 * All margins, padding, and spacing use standardized values.
 */
object AppSpacing {
    // Basic spacing - follows Material Design 3 scale
    val ExtraSmall = 2.dp      // 2dp - micro adjustments
    val Small = 4.dp           // 4dp - tight spacing
    val Tiny = 6.dp            // 6dp - internal component spacing
    val Base = 8.dp            // 8dp - standard spacing unit
    val Compact = 12.dp        // 12dp - compact element spacing
    val Medium = 16.dp         // 16dp - standard padding/margins
    val Large = 24.dp          // 24dp - section spacing
    val ExtraLarge = 32.dp     // 32dp - major sections
    val XXL = 48.dp            // 48dp - full screen margins

    // Specific use cases
    val EdgePadding = Medium   // 16dp - screen edge padding
    val ListItemGap = Base     // 8dp - gap between list items
    val SectionGap = Large     // 24dp - gap between major sections
    val ComponentGap = Tiny    // 6dp - gap between component parts
    val ButtonMinHeight = 48.dp
    val PrimaryButtonHeight = 56.dp
    val BottomBarHeight = 80.dp
    val TopBarHeight = 56.dp
}
