package com.adveno.tv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoFocusBorder
import com.adveno.tv.presentation.theme.AdvenoFocusGlow
import com.adveno.tv.presentation.theme.AdvenoShapes
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoSurfaceVariant

/**
 * Reusable TV Card component.
 * Supports D-pad navigation, focus highlight border, and subtle scaling.
 */
@Composable
fun TvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isFocusable: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && isFocusable) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvCardScale"
    )

    val surfaceColor by animateColorAsState(
        targetValue = if (isFocused && isFocusable) AdvenoSurfaceVariant else AdvenoSurface,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvCardSurface"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused && isFocusable) AdvenoFocusBorder else Color.Transparent,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvCardBorder"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused && isFocusable) AdvenoDimensions.focusedElevation else 2.dp,
                shape = AdvenoShapes.large,
                spotColor = AdvenoFocusGlow
            )
            .clip(AdvenoShapes.large)
            .background(surfaceColor)
            .border(
                width = if (isFocused && isFocusable) AdvenoDimensions.focusBorderWidth else 0.dp,
                color = borderColor,
                shape = AdvenoShapes.large
            )
            .then(clickableModifier)
            .padding(AdvenoDimensions.spaceLg),
        content = content
    )
}

