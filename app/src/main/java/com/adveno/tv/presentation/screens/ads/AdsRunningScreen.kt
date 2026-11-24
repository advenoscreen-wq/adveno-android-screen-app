package com.adveno.tv.presentation.screens.ads

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.adveno.tv.R
import com.adveno.tv.presentation.components.TvLogoutConfirmationModal
import com.adveno.tv.presentation.components.TvPlaybackProgressBar
import com.adveno.tv.presentation.components.TvReportEventsErrorDialog
import com.adveno.tv.presentation.components.TvSignageControlPill
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Stateful entry point for the "MAIN SCREEN: Ads Running".
 *
 * Coordinates:
 * - Full-screen digital signage video (ExoPlayer, 10s) and image (5s) looping playback.
 * - Auto-hiding overlay controls (Logout, Report Events, and Sync buttons) after 3s of user inactivity.
 * - Continuous synchronized thin bottom playback progress bar.
 * - Smooth, flicker-free ad transitions with zero intermediate placeholder flashes.
 * - Remote D-pad, OK button, and touch interaction handling.
 */
@Composable
fun AdsRunningScreen(
    viewModel: AdsRunningViewModel,
    onLogoutClick: () -> Unit,
    onSyncClick: () -> Unit,
    onNavigateToNoAds: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Handle remote Back button: safely show logout confirmation dialog
    BackHandler(enabled = !uiState.isLoggingOut) {
        if (showLogoutConfirmation) {
            showLogoutConfirmation = false
        } else {
            showLogoutConfirmation = true
        }
    }

    // Start initial 3-second auto-hide timer and playlist playback loop when screen opens
    LaunchedEffect(Unit) {
        viewModel.startInactivityTimer()
        viewModel.startPlayback()
    }

    // Auto-navigate to No Ads if backend empty assignment transition completed
    LaunchedEffect(uiState.noAdsAvailable) {
        if (uiState.noAdsAvailable) {
            onNavigateToNoAds()
        }
    }

    AdsRunningScreenContent(
        uiState = uiState,
        showLogoutConfirmation = showLogoutConfirmation,
        onUserInteraction = {
            if (!showLogoutConfirmation) {
                viewModel.onUserInteraction()
            }
        },
        onVideoCompleted = { viewModel.onVideoCompleted() },
        onVideoError = { viewModel.onVideoError() },
        onLogoutClick = {
            showLogoutConfirmation = true
        },
        onConfirmLogout = {
            showLogoutConfirmation = false
            viewModel.logout(onSuccess = onLogoutClick)
        },
        onDismissLogout = {
            showLogoutConfirmation = false
        },
        onSyncClick = onSyncClick,
        onReportEventsClick = {
            viewModel.reportEventsManually()
        },
        onRetryReport = {
            viewModel.reportEventsManually()
        },
        onCancelReport = {
            viewModel.dismissReportError()
        },
        modifier = modifier
    )
}

/**
 * Content Composable for Ads Running.
 * Renders the advertisement media layers, top bar branding, report events, logout button,
 * bottom-right sync button, and bottom progress bar.
 */
@OptIn(UnstableApi::class)
@Composable
fun AdsRunningScreenContent(
    uiState: AdsRunningUiState,
    showLogoutConfirmation: Boolean = false,
    onUserInteraction: () -> Unit,
    onVideoCompleted: () -> Unit,
    onVideoError: () -> Unit,
    onLogoutClick: () -> Unit,
    onConfirmLogout: () -> Unit = {},
    onDismissLogout: () -> Unit = {},
    onSyncClick: () -> Unit,
    onReportEventsClick: () -> Unit = {},
    onRetryReport: () -> Unit = {},
    onCancelReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize ExoPlayer once for the lifetime of this Composable
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE // Loop video if natural duration is shorter than configured duration
        }
    }

    // Bitmap cache for instant, zero-flicker ad transitions
    val imageBitmapCache = remember { mutableMapOf<String, ImageBitmap>() }
    var displayedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }

    // Preload all real image assets in the active playlist into imageBitmapCache
    LaunchedEffect(uiState.playlist) {
        withContext(Dispatchers.IO) {
            for (item in uiState.playlist) {
                if (item is AdItem.Image && !item.filePath.isNullOrBlank()) {
                    val path = item.filePath
                    if (!imageBitmapCache.containsKey(path)) {
                        try {
                            val file = File(path)
                            if (file.exists() && file.canRead()) {
                                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.let { bmp ->
                                    imageBitmapCache[path] = bmp
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // Attach ExoPlayer listener and lifecycle observer with safe release on disposal
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoReady = true
                } else if (playbackState == Player.STATE_ENDED) {
                    onVideoCompleted()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onVideoError()
            }
        }
        exoPlayer.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (uiState.currentItem is AdItem.Video) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // React to active playlist item: prepare/play video from local file or raw resource, or pause
    LaunchedEffect(uiState.currentItem) {
        val item = uiState.currentItem
        if (item is AdItem.Video) {
            isVideoReady = false
            val uri = when {
                !item.filePath.isNullOrBlank() -> Uri.fromFile(File(item.filePath))
                item.rawResId != null -> Uri.parse("rawresource:///${item.rawResId}")
                else -> null
            }

            if (uri != null) {
                try {
                    val mediaItem = MediaItem.fromUri(uri)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(0L)
                    exoPlayer.play()
                } catch (e: Exception) {
                    onVideoError()
                }
            } else {
                onVideoError()
            }
        } else if (item is AdItem.Image) {
            exoPlayer.pause()
            val path = item.filePath
            if (!path.isNullOrBlank()) {
                val cached = imageBitmapCache[path]
                if (cached != null) {
                    displayedBitmap = cached
                } else {
                    withContext(Dispatchers.IO) {
                        try {
                            val file = File(path)
                            if (file.exists() && file.canRead()) {
                                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.let { bmp ->
                                    imageBitmapCache[path] = bmp
                                    withContext(Dispatchers.Main) {
                                        displayedBitmap = bmp
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } else {
            exoPlayer.pause()
        }
    }

    // Crossfade: wait until video has decoded its first frame before bringing videoAlpha up
    val isVideo = uiState.currentItem is AdItem.Video
    val targetVideoAlpha = if (isVideo && isVideoReady) 1f else 0f
    val videoAlpha by animateFloatAsState(
        targetValue = targetVideoAlpha,
        animationSpec = tween(durationMillis = 250),
        label = "VideoCrossfade"
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (targetVideoAlpha == 1f) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
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
            if (videoAlpha > 0.01f) {
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
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = videoAlpha }
                )
            }

            // Image Layer: Smoothly displays preloaded bitmap without ever flashing placeholder
            if (imageAlpha > 0.01f) {
                val currentImage = uiState.currentItem as? AdItem.Image
                val activeBitmap = displayedBitmap

                if (activeBitmap != null) {
                    Image(
                        bitmap = activeBitmap,
                        contentDescription = currentImage?.title ?: "Signage Advertisement",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = imageAlpha }
                    )
                } else if (currentImage?.drawableResId != null) {
                    // Only use embedded resource fallback when running dummy fallback playlist
                    Image(
                        painter = painterResource(id = currentImage.drawableResId),
                        contentDescription = currentImage.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = imageAlpha }
                    )
                }
            }
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
                // Top Bar: Branding (Top-Left) and Actions (Top-Right)
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

                    // Top-Right Actions: Report Events + Logout
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvSignageControlPill(
                            text = if (uiState.isReportingEvents) "Reporting..." else "Report Events",
                            iconRes = R.drawable.ic_cloud_upload,
                            isLoading = uiState.isReportingEvents,
                            onClick = onReportEventsClick
                        )

                        TvSignageControlPill(
                            text = if (uiState.isLoggingOut) "Logging out..." else "Logout",
                            iconRes = R.drawable.ic_logout,
                            isLoading = uiState.isLoggingOut,
                            onClick = onLogoutClick
                        )
                    }
                }

                // Floating Error Notification Banner (e.g. Logout network failure)
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -20 },
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -20 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (isPortrait) 76.dp else 84.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xE60F172A))
                            .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = "Error notification",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Floating Success Notification Banner (e.g. Events Reported confirmation)
                AnimatedVisibility(
                    visible = uiState.reportSuccessMessage != null,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -20 },
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -20 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (isPortrait) 76.dp else 84.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xE60F172A))
                            .border(1.5.dp, AdvenoSuccess, RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Success",
                            tint = AdvenoSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.reportSuccessMessage ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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

        // -------------------------------------------------------------
        // LAYER 4: Logout Confirmation Dialog
        // -------------------------------------------------------------
        TvLogoutConfirmationModal(
            isVisible = showLogoutConfirmation,
            isLoggingOut = uiState.isLoggingOut,
            onConfirm = onConfirmLogout,
            onDismiss = onDismissLogout
        )

        // -------------------------------------------------------------
        // LAYER 5: Playback Event Reporting Error Dialog
        // -------------------------------------------------------------
        TvReportEventsErrorDialog(
            isVisible = uiState.reportErrorMessage != null,
            errorMessage = uiState.reportErrorMessage,
            onRetry = onRetryReport,
            onCancel = onCancelReport
        )
    }
}
