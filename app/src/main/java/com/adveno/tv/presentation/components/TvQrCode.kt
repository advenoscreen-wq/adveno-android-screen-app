package com.adveno.tv.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoFocusBorder
import com.adveno.tv.presentation.theme.AdvenoFocusGlow
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Reusable QR Code display component for Android TV.
 *
 * Characteristics:
 * - High-contrast, sharp black modules on clean pure-white background.
 * - Rounded corners with a soft Adveno cyan/blue glow border.
 * - Smooth entrance fade-in animation.
 * - Perfectly square (1:1 aspect ratio) regardless of screen resolution (720p, 1080p, 4K).
 * - TV Remote focusable with dynamic glow accentuation.
 *
 * To connect a real backend QR generator in the future, simply pass the generated
 * matrix from ZXing or pairing API into the [matrix] parameter.
 */
@Composable
fun TvQrCode(
    modifier: Modifier = Modifier,
    matrix: Array<BooleanArray> = remember { PlaceholderQrGenerator.generatePlaceholderMatrix() },
    contentDescriptionText: String = "Adveno Screen Pairing QR Code",
    cornerRadius: Dp = 22.dp,
    quietZonePadding: Dp = 16.dp,
    isFocusable: Boolean = true,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Smooth entrance fade-in animation
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Gentle scale on TV remote focus
    val scale by animateFloatAsState(
        targetValue = if (isFocused && isFocusable) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = AdvenoDimensions.animDurationMillis),
        label = "TvQrCodeScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val glowAlpha = if (isFocused) 0.65f else 0.35f
    val borderWidth = if (isFocused) 3.dp else 1.5.dp

    val focusModifier = if (isFocusable) {
        if (focusRequester != null) {
            Modifier
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
        } else {
            Modifier.focusable(interactionSource = interactionSource)
        }
    } else Modifier

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .graphicsLayer { alpha = alphaAnim.value }
            .semantics { contentDescription = contentDescriptionText }
            .then(focusModifier)
            // Subtle ambient neon halo around the QR container
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isFocused) AdvenoFocusBorder else AdvenoSecondary).copy(alpha = glowAlpha),
                            AdvenoPrimary.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.68f
                    )
                )
            }
            .shadow(
                elevation = if (isFocused) 18.dp else 10.dp,
                shape = shape,
                spotColor = AdvenoFocusGlow,
                ambientColor = AdvenoPrimary
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = if (isFocused) {
                        listOf(AdvenoFocusBorder, AdvenoSecondary, AdvenoFocusBorder)
                    } else {
                        listOf(
                            AdvenoSecondary.copy(alpha = 0.5f),
                            AdvenoPrimary.copy(alpha = 0.4f)
                        )
                    }
                ),
                shape = shape
            )
            .clip(shape)
            .background(Color.White)
            .padding(quietZonePadding),
        contentAlignment = Alignment.Center
    ) {
        // High-precision canvas module rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val matrixSize = matrix.size
            if (matrixSize == 0) return@Canvas

            // Compute exact module pixel size
            val modulePixelSize = size.minDimension / matrixSize
            val totalGridWidth = modulePixelSize * matrixSize
            val offsetX = (size.width - totalGridWidth) / 2f
            val offsetY = (size.height - totalGridWidth) / 2f

            for (r in 0 until matrixSize) {
                val rowModules = matrix[r]
                for (c in 0 until matrixSize) {
                    if (rowModules[c]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(
                                x = offsetX + (c * modulePixelSize),
                                y = offsetY + (r * modulePixelSize)
                            ),
                            size = Size(
                                width = modulePixelSize + 0.3f, // minimal overlap to prevent hairline seams
                                height = modulePixelSize + 0.3f
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "TvQrCode Preview", showBackground = true, backgroundColor = 0xFF101522)
@Composable
fun TvQrCodePreview() {
    AdvenoTheme {
        Box(
            modifier = Modifier
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            TvQrCode()
        }
    }
}
