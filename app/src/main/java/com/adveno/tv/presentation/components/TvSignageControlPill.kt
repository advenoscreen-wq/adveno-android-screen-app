package com.adveno.tv.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoFocusBorder
import com.adveno.tv.presentation.theme.AdvenoFocusGlow
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary

/**
 * Compact, TV-friendly control button for the digital signage overlay (Logout & Sync buttons).
 * Supports D-pad navigation, focus scaling, cyan neon glow, loading states, and touch clicks.
 */
@Composable
fun TvSignageControlPill(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "ControlPillScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) AdvenoSurface.copy(alpha = 0.95f) else AdvenoSurface.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "ControlPillBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AdvenoFocusBorder else Color.White.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "ControlPillBorder"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isFocused) AdvenoTextPrimary else AdvenoTextSecondary,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "ControlPillContent"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 8.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AdvenoFocusGlow
            )
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
        } else {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
