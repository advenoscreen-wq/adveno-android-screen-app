package com.adveno.tv.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Adveno Design System Shapes.
 * Uses soft, modern rounded corners suitable for high-definition TV screens.
 */
val AdvenoShapes = Shapes(
    small = RoundedCornerShape(8.dp),   // Badges, tags, chips
    medium = RoundedCornerShape(14.dp), // Buttons, dialogs, inputs
    large = RoundedCornerShape(20.dp)   // Cards, preview screens, containers
)

