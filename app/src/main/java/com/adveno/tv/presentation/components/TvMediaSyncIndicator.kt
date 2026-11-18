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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Reusable Ads Synchronization Visual Component.
 *
 * Characteristics:
 * - Rotating neon progress arc surrounding a 3D-styled floating advertisement/media card stack.
 * - Center media card with subtle floating bobbing animation.
 * - Seamless transition to Synced state: media stack fades out, circular ring locks and turns emerald
 *   green (#22C55E), and a crisp success checkmark emerges in the center.
 */
@Composable
fun TvMediaSyncIndicator(
    isSynced: Boolean,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 220.dp,
    strokeWidth: Dp = 7.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TvMediaSyncInfinite")

    // Continuous rotation while syncing
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SyncRotationAngle"
    )

    // Subtle gentle floating bob for the media cards
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MediaCardsFloat"
    )

    // Strictly clockwise rotation while syncing, settled when synced
    val rotationAngle = if (isSynced) 0f else continuousRotation

    // Arc completes to full 360 degrees
    val sweepAngle by animateFloatAsState(
        targetValue = if (isSynced) 360f else 230f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "SweepAngleProgress"
    )

    // Color morphing to Adveno emerald success
    val currentGlowColor by animateColorAsState(
        targetValue = if (isSynced) AdvenoSuccess.copy(alpha = 0.50f) else AdvenoSecondary.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 600),
        label = "GlowColorTransition"
    )

    val currentTrackColor by animateColorAsState(
        targetValue = if (isSynced) AdvenoSuccess.copy(alpha = 0.25f) else AdvenoPrimary.copy(alpha = 0.20f),
        animationSpec = tween(durationMillis = 600),
        label = "TrackColorTransition"
    )

    val successPulseScale by animateFloatAsState(
        targetValue = if (isSynced) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "SuccessPulseScale"
    )

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
                            currentGlowColor.copy(alpha = if (isSynced) 0.35f else 0.22f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.68f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Circular Progress Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx * 2
            val radius = diameter / 2f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Base track
            drawCircle(
                color = currentTrackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            // Rotating / Settled arc
            rotate(degrees = if (isSynced) 0f else rotationAngle, pivot = center) {
                // Glow arc layer
                drawArc(
                    brush = if (isSynced) {
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

                // Foreground neon arc (or solid emerald ring on synced)
                drawArc(
                    brush = if (isSynced) {
                        SolidColor(AdvenoSuccess)
                    } else {
                        Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.25f to AdvenoPrimary.copy(alpha = 0.4f),
                            0.65f to AdvenoPrimary,
                            0.95f to AdvenoSecondary,
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

        // Center Content: Media Cards Stack <-> Success Checkmark Transition
        AnimatedContent(
            targetState = isSynced,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400, delayMillis = 150)) +
                 scaleIn(initialScale = 0.5f, animationSpec = tween(400, delayMillis = 150)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                        scaleOut(targetScale = 0.5f, animationSpec = tween(200))
                    )
            },
            label = "SyncCenterContentTransition"
        ) { synced ->
            if (synced) {
                // Synced State: Clean white checkmark inside emerald ring
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Synced Success Checkmark",
                    tint = Color.White,
                    modifier = Modifier.size((indicatorSize * 0.32f).coerceIn(44.dp, 72.dp))
                )
            } else {
                // Syncing State: 3 Layered Advertisement/Media Cards
                MediaCardsDeck(
                    indicatorSize = indicatorSize,
                    floatOffset = floatOffset
                )
            }
        }
    }
}

/**
 * 3D-styled floating advertisement/media cards deck.
 */
@Composable
private fun MediaCardsDeck(
    indicatorSize: Dp,
    floatOffset: Float
) {
    val cardSize = indicatorSize * 0.38f
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .size(indicatorSize * 0.70f)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back Left Card (Tilted -12 degrees)
        Box(
            modifier = Modifier
                .size(cardSize * 0.88f)
                .offset(x = (-cardSize * 0.48f))
                .rotate(-12f)
                .clip(shape)
                .background(Color(0x551E293B))
                .border(1.dp, AdvenoPrimary.copy(alpha = 0.45f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_media_play),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(cardSize * 0.32f)
            )
        }

        // Back Right Card (Tilted +12 degrees)
        Box(
            modifier = Modifier
                .size(cardSize * 0.88f)
                .offset(x = (cardSize * 0.48f))
                .rotate(12f)
                .clip(shape)
                .background(Color(0x551E293B))
                .border(1.dp, AdvenoPrimary.copy(alpha = 0.45f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_media_play),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(cardSize * 0.32f)
            )
        }

        // Front Center Card (Elevated, primary focus)
        Box(
            modifier = Modifier
                .size(cardSize)
                .shadow(elevation = 12.dp, shape = shape, spotColor = AdvenoSecondary)
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF243048), AdvenoSurface)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(AdvenoSecondary, AdvenoPrimary)
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_media_image),
                contentDescription = "Media Content",
                tint = Color.White,
                modifier = Modifier.size(cardSize * 0.52f)
            )
        }
    }
}

@Preview(name = "Syncing State", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvMediaSyncIndicatorSyncingPreview() {
    AdvenoTheme {
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            TvMediaSyncIndicator(isSynced = false)
        }
    }
}

@Preview(name = "Synced Success State", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvMediaSyncIndicatorSyncedPreview() {
    AdvenoTheme {
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            TvMediaSyncIndicator(isSynced = true)
        }
    }
}

