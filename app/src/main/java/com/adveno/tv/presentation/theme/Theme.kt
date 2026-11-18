package com.adveno.tv.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Adveno Material3 Dark Color Scheme.
 * TV environments require high-contrast dark schemes to prevent eye fatigue.
 */
private val AdvenoColorScheme = darkColorScheme(
    primary = AdvenoPrimary,
    onPrimary = AdvenoTextPrimary,
    secondary = AdvenoSecondary,
    onSecondary = AdvenoBackground,
    background = AdvenoBackground,
    onBackground = AdvenoTextPrimary,
    surface = AdvenoSurface,
    onSurface = AdvenoTextPrimary,
    surfaceVariant = AdvenoSurfaceVariant,
    onSurfaceVariant = AdvenoTextSecondary,
    error = AdvenoError,
    onError = AdvenoTextPrimary
)

@Composable
fun AdvenoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AdvenoColorScheme,
        typography = AdvenoTypography,
        shapes = AdvenoShapes,
        content = content
    )
}
