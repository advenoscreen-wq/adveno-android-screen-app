package com.adveno.tv.presentation.screens.syncing

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
import androidx.compose.foundation.layout.widthIn
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
import com.adveno.tv.presentation.components.TvMediaSyncIndicator
import com.adveno.tv.presentation.components.TvNoInternetModal
import com.adveno.tv.presentation.components.TvProgressBar
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme
import com.adveno.tv.presentation.theme.AdvenoWarning

/**
 * Stateful entry point for the "Ads Synchronization" screen.
 */
@Composable
fun SyncingScreen(
    viewModel: SyncingViewModel,
    onBackClick: () -> Unit,
    onNavigateToAdsRunning: () -> Unit = {},
    onNavigateToNoAds: () -> Unit = {},
    onReturnToAdsRunning: () -> Unit = {},
    onNavigateToStartConnecting: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle Android TV remote Back button
    BackHandler {
        viewModel.reset()
        onBackClick()
    }

    // Launch real backend synchronization workflow on screen start
    LaunchedEffect(Unit) {
        viewModel.startSynchronization()
    }

    // Navigate to Start Connecting if device credential was revoked or disconnected
    LaunchedEffect(uiState.isRevoked) {
        if (uiState.isRevoked) {
            onNavigateToStartConnecting()
        }
    }

    // When manual sync from Ads playback fails, return cleanly to Ads playback without blocking
    LaunchedEffect(uiState.isManualSyncFailed) {
        if (uiState.isManualSyncFailed) {
            onReturnToAdsRunning()
        }
    }

    // When synchronization succeeds: navigate to AdsRunning if Ads exist, or NoAds if none assigned
    LaunchedEffect(uiState.isSynced) {
        if (uiState.isSynced) {
            kotlinx.coroutines.delay(1800L)
            if (uiState.hasAdsAvailable) {
                onNavigateToAdsRunning()
            } else {
                onNavigateToNoAds()
            }
        }
    }

    SyncingScreenContent(
        uiState = uiState,
        onRetryClick = { viewModel.retry() },
        onShowAdsClick = onNavigateToAdsRunning,
        onRetryConnection = { viewModel.retryConnectionFromModal() },
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the Ads Synchronization UI and Recovery Screen.
 */
@Composable
fun SyncingScreenContent(
    uiState: SyncingUiState,
    onRetryClick: () -> Unit = {},
    onShowAdsClick: () -> Unit = {},
    onRetryConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
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
                            if (uiState.isSynced) Color(0x3222C55E)
                            else if (uiState.isRecoveryMode) Color(0x28F59E0B)
                            else Color(0x2822D3EE),
                            if (uiState.isSynced) Color(0x1815803D)
                            else if (uiState.isRecoveryMode) Color(0x18B45309)
                            else Color(0x143478F6),
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

        // Adaptive indicator sizing
        val indicatorSize = if (isPortrait) {
            (minOf(screenWidth * 0.50f, screenHeight * 0.28f)).coerceIn(180.dp, 260.dp)
        } else {
            (screenHeight * 0.36f).coerceIn(160.dp, 240.dp)
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

                Spacer(modifier = Modifier.height(if (isPortrait) 42.dp else 30.dp))

                // Central Animated Media Sync Visual
                TvMediaSyncIndicator(
                    isSynced = uiState.isSynced,
                    indicatorSize = indicatorSize,
                    strokeWidth = if (isPortrait) 8.dp else 7.dp
                )

                Spacer(modifier = Modifier.height(if (isPortrait) 36.dp else 24.dp))

                if (uiState.isRecoveryMode) {
                    // =========================================================
                    // RECOVERY MODE UI: Initial Setup Failure Recovery
                    // =========================================================
                    Text(
                        text = uiState.recoveryTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AdvenoTextPrimary,
                        fontSize = if (isPortrait) 32.sp else 34.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.recoveryMessage,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AdvenoTextSecondary,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isPortrait) 20.dp else 48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Need assistance? Contact Adveno support at support@adveno.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdvenoWarning,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // TV Action buttons: Show Ads (if local Ads exist) and Sync Now
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.hasUsableLocalAds) {
                            TvButton(
                                text = "Show Ads",
                                onClick = onShowAdsClick,
                                modifier = Modifier.widthIn(min = 180.dp)
                            )
                        }
                        TvButton(
                            text = "Sync Now",
                            onClick = onRetryClick,
                            modifier = Modifier.widthIn(min = 180.dp)
                        )
                    }
                } else {
                    // =========================================================
                    // NORMAL PROGRESS UI: Live download, progress bar & retries
                    // =========================================================
                    AnimatedContent(
                        targetState = uiState.heading,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { -15 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(250)) + slideOutVertically(animationSpec = tween(250)) { 15 }
                                )
                        },
                        label = "SyncHeadingTransition"
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

                    AnimatedContent(
                        targetState = uiState.description,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(500, delayMillis = 150)) + slideInVertically(animationSpec = tween(500, delayMillis = 150)) { 15 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200))
                                )
                        },
                        label = "SyncDescriptionTransition"
                    ) { descText ->
                        Text(
                            text = descText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = AdvenoTextSecondary,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isPortrait) 20.dp else 48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isPortrait) 36.dp else 28.dp))

                    // Horizontal Progress Bar & Percentage
                    Row(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth(if (isPortrait) 0.88f else 0.52f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TvProgressBar(
                            progress = uiState.progress,
                            isSynced = uiState.isSynced,
                            modifier = Modifier.weight(1f),
                            height = 14.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = uiState.percentageText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isSynced) AdvenoSuccess else AdvenoTextPrimary,
                            fontSize = 17.sp,
                            modifier = Modifier.widthIn(min = 44.dp)
                        )
                    }
                }
            }
        }

        // Overlay No Internet Connection modal when network is interrupted
        TvNoInternetModal(
            isVisible = uiState.isNoInternetModalVisible,
            interruptedOperation = uiState.interruptedOperation,
            statusMessage = uiState.noInternetStatusMessage,
            isConnectionRestored = uiState.isConnectionRestored,
            onRetryConnection = onRetryConnection
        )
    }
}

// -----------------------------------------------------------------------------------------
// Previews
// -----------------------------------------------------------------------------------------

@Preview(name = "Landscape - Syncing 70%", widthDp = 960, heightDp = 540, showBackground = true)
@Composable
fun SyncingLandscapePreview() {
    AdvenoTheme {
        SyncingScreenContent(
            uiState = SyncingUiState(isSynced = false, progress = 0.70f)
        )
    }
}

@Preview(name = "Landscape - Recovery Mode", widthDp = 960, heightDp = 540, showBackground = true)
@Composable
fun RecoveryModeLandscapePreview() {
    AdvenoTheme {
        SyncingScreenContent(
            uiState = SyncingUiState(
                isRecoveryMode = true,
                hasUsableLocalAds = false,
                recoveryTitle = "Unable to synchronize your Ads.",
                recoveryMessage = "Failed to download Ads after multiple attempts. Please contact Adveno support or sync to retry."
            )
        )
    }
}

@Preview(name = "Landscape - Synced 100%", widthDp = 960, heightDp = 540, showBackground = true)
@Composable
fun SyncedLandscapePreview() {
    AdvenoTheme {
        SyncingScreenContent(
            uiState = SyncingUiState(isSynced = true, progress = 1.0f)
        )
    }
}

@Preview(name = "Portrait - Syncing 70%", widthDp = 540, heightDp = 960, showBackground = true)
@Composable
fun SyncingPortraitPreview() {
    AdvenoTheme {
        SyncingScreenContent(
            uiState = SyncingUiState(isSynced = false, progress = 0.70f)
        )
    }
}

@Preview(name = "Portrait - Recovery Mode", widthDp = 540, heightDp = 960, showBackground = true)
@Composable
fun RecoveryModePortraitPreview() {
    AdvenoTheme {
        SyncingScreenContent(
            uiState = SyncingUiState(
                isRecoveryMode = true,
                hasUsableLocalAds = false,
                recoveryTitle = "Unable to synchronize your Ads.",
                recoveryMessage = "Failed to retrieve Ads after multiple attempts. Please contact Adveno support or sync to retry."
            )
        )
    }
}
