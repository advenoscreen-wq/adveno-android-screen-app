package com.adveno.tv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.presentation.theme.AdvenoButtonFocused
import com.adveno.tv.presentation.theme.AdvenoButtonUnfocused
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoFocusBorder
import com.adveno.tv.presentation.theme.AdvenoFocusGlow
import com.adveno.tv.presentation.theme.AdvenoShapes
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary

/**
 * Premium Android TV Action Button.
 *
 * Implements full D-pad TV focus behavior:
 * - 8% scale increase when highlighted
 * - Glowing 3dp cyan focus ring
 * - Smooth color interpolation between dark surface and vibrant electric blue
 * - Elevation shadow spotlight
 * - Optional loading state with progress spinner
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth D-pad focus animations
    val scale by animateFloatAsState(
        targetValue = if (isFocused) AdvenoDimensions.focusedScale else AdvenoDimensions.defaultScale,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvButtonScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> AdvenoButtonUnfocused.copy(alpha = 0.5f)
            isFocused -> AdvenoButtonFocused
            else -> AdvenoButtonUnfocused
        },
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvButtonBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AdvenoFocusBorder else Color.Transparent,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvButtonBorder"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = AdvenoShapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = AdvenoTextPrimary,
            disabledContainerColor = AdvenoButtonUnfocused.copy(alpha = 0.5f),
            disabledContentColor = AdvenoTextSecondary.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = AdvenoDimensions.unfocusedElevation,
            focusedElevation = AdvenoDimensions.focusedElevation
        ),
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) AdvenoDimensions.focusedElevation else 0.dp,
                shape = AdvenoShapes.medium,
                spotColor = AdvenoFocusGlow,
                ambientColor = AdvenoFocusGlow
            )
            .border(
                width = if (isFocused) AdvenoDimensions.focusBorderWidth else 0.dp,
                color = borderColor,
                shape = AdvenoShapes.medium
            )
            .defaultMinSize(minHeight = AdvenoDimensions.buttonMinHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(
                horizontal = AdvenoDimensions.buttonHorizontalPadding,
                vertical = AdvenoDimensions.buttonVerticalPadding
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AdvenoTextPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AdvenoDimensions.spaceSm))
            } else if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(AdvenoDimensions.spaceSm))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
