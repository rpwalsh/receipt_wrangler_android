/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

// Elevation constants for layering and depth
object AppElevation {
    val none = 0.dp
    val overlay = 1.dp          // Subtle backgrounds
    val card = 2.dp             // Standard cards
    val raised = 4.dp           // Buttons, interactive elements
    val floating = 6.dp         // FABs, floating panels
    val modal = 8.dp            // Dialogs, dropdowns
    val premium = 12.dp         // Premium highlighted elements
}
