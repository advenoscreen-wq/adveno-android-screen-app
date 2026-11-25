package com.adveno.tv.presentation.screens.ads

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.KeyEvent
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.adveno.tv.presentation.components.AdminPinDialog
import com.adveno.tv.presentation.components.TvLogoutConfirmationModal
import com.adveno.tv.presentation.components.TvPlaybackProgressBar
import com.adveno.tv.presentation.components.TvReportEventsErrorDialog
import com.adveno.tv.presentation.components.TvSignageControlPill
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    onNavigateToProtectedSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // In kiosk mode, remote back button does not exit the app or show logout
    BackHandler(enabled = !uiState.isLoggingOut) {
        if (showLogoutConfirmation) {
            showLogoutConfirmation = false
        }
    }

    // Start initial 3-second auto-hide timer and playlist playback loop when screen opens
    LaunchedEffect(Unit) {
        viewModel.startInactivityTimer()
        viewModel.startPlayback()
    }

    // Auto-navigate to No Ads if backend empty assignment transition completed and no ads exist
    LaunchedEffect(uiState.noAdsAvailable, uiState.playlist) {
        if (uiState.noAdsAvailable && uiState.playlist.isEmpty()) {
            onNavigateToNoAds()
        }
    }

    AdsRunningScreenContent(
        uiState = uiState,
        showLogoutConfirmation = showLogoutConfirmation,
        isVerifyingPin = viewModel.isVerifyingPin,
        pinErrorMessage = viewModel.pinErrorMessage,
        onVerifyPin = { pin, onSuccess ->
            viewModel.verifyAdminPin(pin, onSuccess)
        },
        onClearPinError = {
            viewModel.clearPinError()
        },
        onNavigateToProtectedSettings = onNavigateToProtectedSettings,
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
 * Kiosk-style digital signage ad display with:
 * - Advertisement media layers (ExoPlayer video + image crossfade)
 * - Auto-hiding overlay branding (no visible administrative controls)
 * - 5-second Settings/Menu remote key hold detection for Protected Settings
 * - Dedicated Admin PIN dialog
 * - Continuous bottom playback progress bar.
 */
@OptIn(UnstableApi::class)
@Composable
fun AdsRunningScreenContent(
    uiState: AdsRunningUiState,
    showLogoutConfirmation: Boolean = false,
    isVerifyingPin: Boolean = false,
    pinErrorMessage: String? = null,
    onVerifyPin: (String, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    onClearPinError: () -> Unit = {},
    onNavigateToProtectedSettings: () -> Unit = {},
    onUserInteraction: () -> Unit,
    onVideoCompleted: () -> Unit,
    onVideoError: () -> Unit,
    onLogoutClick: () -> Unit = {},
    onConfirmLogout: () -> Unit = {},
    onDismissLogout: () -> Unit = {},
    onSyncClick: () -> Unit = {},
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

    // Admin PIN & 5-second D-pad OK/Center key hold state
    var isHoldingOkKey by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var hasHoldCompleted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var holdJob by remember { mutableStateOf<Job?>(null) }

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
            // Capture TV remote key events (D-pad OK hold for settings, normal navigation, etc.)
            .onKeyEvent { keyEvent ->
                val nativeEvent = keyEvent.nativeKeyEvent
                val keyCode = nativeEvent.keyCode
                val isOkKey = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

                if (isOkKey) {
                    if (nativeEvent.action == KeyEvent.ACTION_DOWN) {
                        if (!showAdminPinDialog) {
                            if (nativeEvent.repeatCount == 0 && !isHoldingOkKey) {
                                isHoldingOkKey = true
                                hasHoldCompleted = false
                                holdProgress = 0f
                                holdJob?.cancel()
                                holdJob = coroutineScope.launch {
                                    val startTime = System.currentTimeMillis()
                                    val totalDuration = 3000L
                                    while (isActive) {
                                        delay(50L)
                                        val elapsed = System.currentTimeMillis() - startTime
                                        holdProgress = (elapsed / totalDuration.toFloat()).coerceIn(0f, 1f)
                                        if (elapsed >= totalDuration) {
                                            hasHoldCompleted = true
                                            isHoldingOkKey = false
                                            holdProgress = 0f
                                            showAdminPinDialog = true
                                            break
                                        }
                                    }
                                }
                            }
                            // Consume key event during the hold so normal OK action is not triggered
                            return@onKeyEvent true
                        }
                    } else if (nativeEvent.action == KeyEvent.ACTION_UP) {
                        holdJob?.cancel()
                        holdJob = null
                        val wasHolding = isHoldingOkKey
                        val completed = hasHoldCompleted
                        isHoldingOkKey = false
                        holdProgress = 0f

                        if (completed) {
                            // Hold completed and opened the dialog: consume this release so it never clicks dialog buttons!
                            hasHoldCompleted = false
                            return@onKeyEvent true
                        }

                        if (!showAdminPinDialog && wasHolding) {
                            // Short OK press while dialog was not open: normal app behavior
                            onUserInteraction()
                            return@onKeyEvent true
                        }
                    }
                }

                if (!showAdminPinDialog) {
                    onUserInteraction()
                }
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
        // LAYER 2: Overlay Controls (Top Bar Branding & Status Notifications)
        // Kiosk-style: No visible administrative controls (Sync/Logout/Report).
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar: Branding Only (Top-Left)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(
                            horizontal = if (isPortrait) 20.dp else AdvenoDimensions.spaceXl,
                            vertical = 24.dp
                        ),
                    horizontalArrangement = Arrangement.Start,
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

        // -------------------------------------------------------------
        // LAYER 6: Subtle 5-second Hold Indicator for Protected Settings
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isHoldingOkKey && !showAdminPinDialog,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE6101522))
                    .border(1.dp, Color(0xFF3478F6).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                CircularProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF3478F6),
                    trackColor = Color(0x333478F6),
                    strokeWidth = 2.5.dp
                )
                val remainingSeconds = kotlin.math.ceil((3000L * (1f - holdProgress)) / 1000f).toInt().coerceAtLeast(1)
                Text(
                    text = "Hold for Settings (${remainingSeconds}s)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // -------------------------------------------------------------
        // LAYER 7: Dedicated Admin PIN Dialog
        // -------------------------------------------------------------
        AdminPinDialog(
            isVisible = showAdminPinDialog,
            isLoading = isVerifyingPin,
            errorMessage = pinErrorMessage,
            onSubmitPin = { pin ->
                onVerifyPin(pin) {
                    showAdminPinDialog = false
                    onNavigateToProtectedSettings()
                }
            },
            onCancel = {
                showAdminPinDialog = false
                onClearPinError()
            }
        )
    }
}
