package com.adveno.tv.presentation.screens.connecting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.R
import com.adveno.tv.presentation.components.TvButton
import com.adveno.tv.presentation.components.TvConnectingIndicator
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme
import kotlinx.coroutines.delay

/**
 * Stateful entry point for the "Connecting / Pairing & Success" screen.
 * Seamlessly manages both the 5-second rotating loading state and the final "Connected!" state.
 */
@Composable
fun ConnectingScreen(
    viewModel: ConnectingViewModel,
    onBackClick: () -> Unit,
    onNavigateToSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle Android TV remote Back button to return to Scan to Connect
    BackHandler {
        viewModel.reset()
        onBackClick()
    }

    // Collect navigation events when final pairing succeeds
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ConnectingNavigation.NavigateToSync -> {
                    onNavigateToSync()
                }
            }
        }
    }

    // Execute backend pairing API for active session
    LaunchedEffect(Unit) {
        viewModel.startPairingExecution()
    }

    ConnectingScreenContent(
        uiState = uiState,
        onRetryClick = { viewModel.retry() },
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the Connecting Animation & Connected Success UI.
 *
 * Responsiveness:
 * - Landscape (16:9): Centered branding, prominent progress indicator with morphing icon,
 *   smoothly animated heading and description.
 * - Portrait (9:16): Vertical proportional stack optimized for signage displays.
 * - Deep dark slate canvas with signature Adveno cyan/indigo or emerald ambient lighting.
 */
@Composable
fun ConnectingScreenContent(
    uiState: ConnectingUiState,
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
    }
    LaunchedEffect(Unit) {
        entranceOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 400))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AdvenoBackground)
            // Atmospheric brand lighting gradients
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x388B5CF6),
                            Color(0x184F46E5),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.2f),
                        radius = size.minDimension * 0.75f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (uiState.isConnected) Color(0x3222C55E) else Color(0x2822D3EE),
                            if (uiState.isConnected) Color(0x1815803D) else Color(0x143478F6),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.minDimension * 0.7f
                    )
                )
            }
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isPortrait = screenHeight > screenWidth

        val horizontalPadding = if (isPortrait) 32.dp else AdvenoDimensions.tvHorizontalPadding
        val verticalPadding = if (isPortrait) 36.dp else AdvenoDimensions.tvVerticalPadding

        // Adaptive circular indicator size
        val indicatorSize = if (isPortrait) {
            (minOf(screenWidth * 0.50f, screenHeight * 0.28f)).coerceIn(180.dp, 260.dp)
        } else {
            (screenHeight * 0.38f).coerceIn(160.dp, 250.dp)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffsetY.value
                    }
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered Adveno Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_adveno_logo),
                        contentDescription = "Adveno Brand Logo",
                        modifier = Modifier.size(if (isPortrait) 52.dp else 48.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = uiState.brandName,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdvenoTextPrimary,
                        fontSize = if (isPortrait) 42.sp else 40.sp
                    )
                }

                Spacer(modifier = Modifier.height(if (isPortrait) 48.dp else 36.dp))

                // Progress Indicator (Rotating arc -> Completed solid emerald success ring with checkmark)
                TvConnectingIndicator(
                    isConnected = uiState.isConnected,
                    indicatorSize = indicatorSize,
                    strokeWidth = if (isPortrait) 8.dp else 7.dp
                )

                Spacer(modifier = Modifier.height(if (isPortrait) 40.dp else 28.dp))

                // Heading: Smoothly transitions from "Connecting..." to "Connected!" without layout jumps
                AnimatedContent(
                    targetState = uiState.heading,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { -15 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(250)) + slideOutVertically(animationSpec = tween(250)) { 15 }
                            )
                    },
                    label = "HeadingSuccessTransition"
                ) { headingText ->
                    Text(
                        text = headingText,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AdvenoTextPrimary,
                        fontSize = if (isPortrait) 34.sp else 36.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description: Smoothly fades and slides from "Linking..." to "Your screen is now connected..."
                AnimatedContent(
                    targetState = uiState.description,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(500, delayMillis = 150)) + slideInVertically(animationSpec = tween(500, delayMillis = 150)) { 15 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(200))
                            )
                    },
                    label = "DescriptionSuccessTransition"
                ) { descText ->
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AdvenoTextSecondary,
                        fontSize = if (isPortrait) 18.sp else 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isPortrait) 20.dp else 48.dp)
                    )
                }

                // Error message and retry button when backend registration fails
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    TvButton(
                        text = "Retry Connection",
                        onClick = onRetryClick,
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Previews for Both Orientations and States
// -----------------------------------------------------------------------------------------

@Preview(
    name = "Landscape - Connecting",
    widthDp = 960,
    heightDp = 540,
    showBackground = true
)
@Composable
fun ConnectingLandscapePreview() {
    AdvenoTheme {
        ConnectingScreenContent(
            uiState = ConnectingUiState(isConnected = false)
        )
    }
}

@Preview(
    name = "Landscape - Connected Success",
    widthDp = 960,
    heightDp = 540,
    showBackground = true
)
@Composable
fun ConnectedLandscapePreview() {
    AdvenoTheme {
        ConnectingScreenContent(
            uiState = ConnectingUiState(isConnected = true)
        )
    }
}

@Preview(
    name = "Portrait - Connecting",
    widthDp = 540,
    heightDp = 960,
    showBackground = true
)
@Composable
fun ConnectingPortraitPreview() {
    AdvenoTheme {
        ConnectingScreenContent(
            uiState = ConnectingUiState(isConnected = false)
        )
    }
}

@Preview(
    name = "Portrait - Connected Success",
    widthDp = 540,
    heightDp = 960,
    showBackground = true
)
@Composable
fun ConnectedPortraitPreview() {
    AdvenoTheme {
        ConnectingScreenContent(
            uiState = ConnectingUiState(isConnected = true)
        )
    }
}
