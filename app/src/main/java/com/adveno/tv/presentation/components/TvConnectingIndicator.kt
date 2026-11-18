package com.adveno.tv.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adveno.tv.R
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Premium Android TV Pairing & Success Indicator.
 *
 * Visual Transitions:
 * - Connecting Phase: Circular track with infinite rotating neon gradient arc and stationary link icon.
 * - Transition Phase: Arc completes the circle (230° -> 360°), rotation settles, colors morph to Adveno
 *   emerald success green (#22C55E), and link icon smoothly transitions to checkmark.
 * - Connected Phase: Complete static emerald success ring with centered checkmark, subtle glow, zero rotation.
 */
@Composable
fun TvConnectingIndicator(
    modifier: Modifier = Modifier,
    isConnected: Boolean = false,
    indicatorSize: Dp = 200.dp,
    strokeWidth: Dp = 7.dp,
    trackColor: Color = AdvenoPrimary.copy(alpha = 0.20f),
    glowColor: Color = AdvenoSecondary.copy(alpha = 0.45f)
) {
    // Continuous rotation while connecting - brisk and fluid rotation speed
    val infiniteTransition = rememberInfiniteTransition(label = "TvConnectingTransition")
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ConnectingRotationAngle"
    )

    // Smoothly stop rotation when connected
    val rotationAngle by animateFloatAsState(
        targetValue = if (isConnected) 360f else continuousRotation,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "SettledRotationAngle"
    )

    // Arc completes its circle (230 degrees -> 360 degrees full circle)
    val sweepAngle by animateFloatAsState(
        targetValue = if (isConnected) 360f else 230f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "SweepAngleProgress"
    )

    // Color morphing to Adveno success emerald green
    val currentTrackColor by animateColorAsState(
        targetValue = if (isConnected) AdvenoSuccess.copy(alpha = 0.25f) else trackColor,
        animationSpec = tween(durationMillis = 600),
        label = "TrackColorTransition"
    )

    val currentGlowColor by animateColorAsState(
        targetValue = if (isConnected) AdvenoSuccess.copy(alpha = 0.50f) else glowColor,
        animationSpec = tween(durationMillis = 600),
        label = "GlowColorTransition"
    )

    val currentPrimaryRingColor by animateColorAsState(
        targetValue = if (isConnected) AdvenoSuccess else AdvenoPrimary,
        animationSpec = tween(durationMillis = 600),
        label = "PrimaryRingColorTransition"
    )

    val currentSecondaryRingColor by animateColorAsState(
        targetValue = if (isConnected) AdvenoSuccess else AdvenoSecondary,
        animationSpec = tween(durationMillis = 600),
        label = "SecondaryRingColorTransition"
    )

    // Gentle success pulse
    val successPulseScale by animateFloatAsState(
        targetValue = if (isConnected) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "SuccessPulseScale"
    )

    val iconSize = (indicatorSize * 0.28f).coerceIn(36.dp, 64.dp)

    Box(
        modifier = modifier
            .size(indicatorSize)
            .aspectRatio(1f)
            .scale(successPulseScale)
            // Atmospheric ambient glow halo behind the indicator
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            currentGlowColor.copy(alpha = if (isConnected) 0.35f else 0.22f),
                            currentPrimaryRingColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.68f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx * 2
            val radius = diameter / 2f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // 1. Base track ring
            drawCircle(
                color = currentTrackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            // 2. Rotating / settled arc
            rotate(degrees = if (isConnected) 0f else rotationAngle, pivot = center) {
                // Glow arc layer
                drawArc(
                    brush = if (isConnected) {
                        SolidColor(AdvenoSuccess.copy(alpha = 0.45f))
                    } else {
                        Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.4f to currentGlowColor.copy(alpha = 0.2f),
                            0.7f to currentGlowColor,
                            1.0f to Color.Transparent
                        )
                    },
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx * 2.2f, cap = StrokeCap.Round)
                )

                // Main foreground arc (or solid emerald success ring)
                drawArc(
                    brush = if (isConnected) {
                        SolidColor(AdvenoSuccess)
                    } else {
                        Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.25f to currentPrimaryRingColor.copy(alpha = 0.4f),
                            0.65f to currentPrimaryRingColor,
                            0.95f to currentSecondaryRingColor,
                            1.0f to Color.White
                        )
                    },
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Central Icon transition: smoothly transforms link into checkmark
        AnimatedContent(
            targetState = isConnected,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400, delayMillis = 150)) +
                 scaleIn(initialScale = 0.5f, animationSpec = tween(400, delayMillis = 150)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                        scaleOut(targetScale = 0.5f, animationSpec = tween(200))
                    )
            },
            label = "CenterIconTransition"
        ) { connected ->
            if (connected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Connected Success Checkmark",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize * 1.15f)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_link),
                    contentDescription = "Connecting Icon",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Preview(name = "Connecting State", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvConnectingIndicatorConnectingPreview() {
    AdvenoTheme {
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            TvConnectingIndicator(isConnected = false)
        }
    }
}

@Preview(name = "Connected Success State", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvConnectingIndicatorConnectedPreview() {
    AdvenoTheme {
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            TvConnectingIndicator(isConnected = true)
        }
    }
}
