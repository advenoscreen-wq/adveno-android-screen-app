package com.adveno.tv.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSurface

/**
 * Thin, high-precision playback progress bar for the digital signage advertisement player.
 * Displays the current progress (0% to 100%) of the actively playing advertisement item.
 */
@Composable
fun TvPlaybackProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = AdvenoSurface.copy(alpha = 0.7f),
    startColor: Color = AdvenoPrimary,
    endColor: Color = AdvenoSecondary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 60),
        label = "PlaybackProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(startColor, endColor)
                        )
                    )
            )
        }
    }
}

