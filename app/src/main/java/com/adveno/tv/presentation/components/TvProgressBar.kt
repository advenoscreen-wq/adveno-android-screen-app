package com.adveno.tv.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Reusable Horizontal Progress Bar for Android TV.
 *
 * Characteristics:
 * - Rounded track with semi-transparent dark surface.
 * - Dynamic animated gradient fill: electric blue to cyan while syncing; turns emerald green on sync complete.
 * - Subtle neon glow running along the progress fill.
 */
@Composable
fun TvProgressBar(
    progress: Float,
    isSynced: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    trackColor: Color = Color(0x33243048)
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val glowColor by animateColorAsState(
        targetValue = if (isSynced) AdvenoSuccess.copy(alpha = 0.5f) else AdvenoSecondary.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 500),
        label = "ProgressBarGlowColor"
    )

    val shape = RoundedCornerShape(height / 2f)

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedProgress)
                    .clip(shape)
                    // Ambient glow shadow along the fill
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent),
                                center = Offset(size.width, size.height / 2f),
                                radius = size.height * 2.5f
                            ),
                            size = size,
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                    .background(
                        brush = if (isSynced) {
                            Brush.horizontalGradient(
                                listOf(AdvenoSuccess.copy(alpha = 0.85f), AdvenoSuccess)
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(AdvenoPrimary, AdvenoSecondary)
                            )
                        }
                    )
            )
        }
    }
}

@Preview(name = "TvProgressBar Syncing", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvProgressBarSyncingPreview() {
    AdvenoTheme {
        TvProgressBar(
            progress = 0.70f,
            isSynced = false,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Preview(name = "TvProgressBar Synced", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvProgressBarSyncedPreview() {
    AdvenoTheme {
        TvProgressBar(
            progress = 1.0f,
            isSynced = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

