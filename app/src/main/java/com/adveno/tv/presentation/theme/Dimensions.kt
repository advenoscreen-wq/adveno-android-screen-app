package com.adveno.tv.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard spacing & dimension tokens for the Adveno TV 10-foot experience.
 * Scales automatically across 720p, 1080p, and 4K displays via Android dp units.
 */
object AdvenoDimensions {
    // Spacing
    val spaceXs: Dp = 4.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 16.dp
    val spaceLg: Dp = 24.dp
    val spaceXl: Dp = 32.dp
    val spaceXxl: Dp = 48.dp
    val spaceHero: Dp = 64.dp

    // Safe Area Margins for Television screens (Overscan margin protection)
    val tvHorizontalPadding: Dp = 56.dp
    val tvVerticalPadding: Dp = 48.dp

    // Component Sizes
    val buttonMinHeight: Dp = 56.dp
    val buttonHorizontalPadding: Dp = 32.dp
    val buttonVerticalPadding: Dp = 16.dp
    val focusBorderWidth: Dp = 3.dp
    val focusedElevation: Dp = 16.dp
    val unfocusedElevation: Dp = 0.dp

    // Focus Animation Scale
    const val focusedScale: Float = 1.08f
    const val defaultScale: Float = 1.0f
    const val animDurationMillis: Int = 200
}

