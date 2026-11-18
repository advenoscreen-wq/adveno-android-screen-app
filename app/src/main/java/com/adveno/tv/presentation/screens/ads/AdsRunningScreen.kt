package com.adveno.tv.presentation.screens.ads

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.adveno.tv.R
import com.adveno.tv.presentation.components.TvPlaybackProgressBar
import com.adveno.tv.presentation.components.TvSignageControlPill
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import kotlinx.coroutines.delay

/**
 * Stateful entry point for the "MAIN SCREEN: Ads Running".
 *
 * Coordinates:
 * - Full-screen digital signage video (ExoPlayer, 10s) and image (5s) looping playback.
 * - Auto-hiding overlay controls (Logout and Sync buttons) after 3s of user inactivity.
 * - Continuous synchronized thin bottom playback progress bar.
 * - Remote D-pad, OK button, and touch interaction handling.
 */
@Composable
fun AdsRunningScreen(
    viewModel: AdsRunningViewModel,
    onLogoutClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle remote Back button: safely logout / clear back stack
    BackHandler {
        viewModel.reset()
        onLogoutClick()
    }

    // Start initial 3-second auto-hide timer and playback loop when screen opens
    LaunchedEffect(Unit) {
        viewModel.startInactivityTimer()
        viewModel.startPlayback()
    }

    AdsRunningScreenContent(
        uiState = uiState,
        onUserInteraction = { viewModel.onUserInteraction() },
        onVideoProgress = { pos, dur -> viewModel.updateVideoProgress(pos, dur) },
        onVideoCompleted = { viewModel.onVideoCompleted() },
        onVideoError = { viewModel.onVideoError() },
        onLogoutClick = {
            viewModel.reset()
            onLogoutClick()
        },
        onSyncClick = onSyncClick,
        modifier = modifier
    )
}

/**
 * Content Composable for Ads Running.
 * Renders the advertisement media layers, top bar branding and logout button,
 * bottom-right sync button, and bottom progress bar.
 */
@OptIn(UnstableApi::class)
@Composable
fun AdsRunningScreenContent(
    uiState: AdsRunningUiState,
    onUserInteraction: () -> Unit,
    onVideoProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onVideoCompleted: () -> Unit,
    onVideoError: () -> Unit,
    onLogoutClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize ExoPlayer once for the lifetime of this Composable
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Prepare local MP4 video media item with 10s clipping using canonical raw resource URI
    @Suppress("DEPRECATION")
    val rawUri = remember {
        RawResourceDataSource.buildRawResourceUri(R.raw.sample_ad_video)
    }
    val mediaItem = remember(rawUri) {
        MediaItem.Builder()
            .setUri(rawUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(10_000L)
                    .build()
            )
            .build()
    }

    // Attach ExoPlayer listener and ensure safe release on disposal
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoCompleted()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onVideoError()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // React to active playlist item: prepare/play video or pause
    LaunchedEffect(uiState.currentItem) {
        if (uiState.currentItem is AdItem.Video) {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.seekTo(0L)
            exoPlayer.play()

            // Fine-tune progress bar from player position while video plays
            while (true) {
                delay(50L)
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (dur > 0L) {
                    onVideoProgress(pos, dur)
                }
                if (exoPlayer.playbackState == Player.STATE_ENDED || (dur > 0L && pos >= dur)) {
                    break
                }
            }
        } else {
            exoPlayer.pause()
        }
    }

    // Crossfade alpha between video and image to eliminate any black flash
    val isVideo = uiState.currentItem is AdItem.Video
    val videoAlpha by animateFloatAsState(
        targetValue = if (isVideo) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "VideoCrossfade"
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (!isVideo) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "ImageCrossfade"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AdvenoBackground)
            // Capture touch interactions
            .pointerInput(Unit) {
                detectTapGestures {
                    onUserInteraction()
                }
            }
            // Capture TV remote key events (D-pad, OK, etc.)
            .onKeyEvent {
                onUserInteraction()
                false
            }
            .focusable()
    ) {
        val isPortrait = maxHeight > maxWidth

        // -------------------------------------------------------------
        // LAYER 1: Full-screen Advertisement Display Area
        // -------------------------------------------------------------
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Video Layer (AndroidX Media3 ExoPlayer with TextureView layout)
            AndroidView(
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(
                        R.layout.view_signage_player,
                        null,
                        false
                    ) as PlayerView
                    view.player = exoPlayer
                    view
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = videoAlpha }
            )

            // Image Layer (Jetpack Compose Image)
            Image(
                painter = painterResource(id = R.drawable.sample_ad_image),
                contentDescription = "Signage Advertisement",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = imageAlpha }
            )
        }

        // -------------------------------------------------------------
        // LAYER 2: Overlay Controls (Top Bar + Sync Button)
        // Auto-hides after 3 seconds of inactivity
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar: Branding (Top-Left) and Logout Button (Top-Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(
                            horizontal = if (isPortrait) 20.dp else AdvenoDimensions.spaceXl,
                            vertical = 24.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left Branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_adveno_logo),
                            contentDescription = "Adveno Logo",
                            modifier = Modifier.size(if (isPortrait) 24.dp else 28.dp)
                        )
                        Text(
                            text = "Adveno",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Top-Right Logout Button
                    TvSignageControlPill(
                        text = "Logout",
                        iconRes = R.drawable.ic_logout,
                        onClick = onLogoutClick
                    )
                }

                // Bottom-Right Sync Button
                TvSignageControlPill(
                    text = "Sync",
                    iconRes = R.drawable.ic_refresh,
                    onClick = onSyncClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = if (isPortrait) 20.dp else AdvenoDimensions.spaceXl,
                            bottom = if (isPortrait) 24.dp else 28.dp
                        )
                )
            }
        }

        // -------------------------------------------------------------
        // LAYER 3: Playback Progress Bar (Always Visible at Bottom)
        // Resets for each advertisement item (Video 10s / Image 5s)
        // -------------------------------------------------------------
        TvPlaybackProgressBar(
            progress = uiState.currentProgress,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
