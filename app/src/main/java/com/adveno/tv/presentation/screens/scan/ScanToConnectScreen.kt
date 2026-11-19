package com.adveno.tv.presentation.screens.scan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.focus.FocusRequester
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
import com.adveno.tv.presentation.components.TvQrCode
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Stateful entry point for the "Scan to Connect" screen.
 */
@Composable
fun ScanToConnectScreen(
    viewModel: ScanToConnectViewModel,
    onBackClick: () -> Unit,
    onAutoNavigateToConnecting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle Android TV remote Back button to return to Connect Your Screen
    BackHandler {
        viewModel.stopPolling()
        onBackClick()
    }

    // Collect navigation events triggered by backend status changes
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ScanToConnectNavigation.NavigateToConnecting -> {
                    onAutoNavigateToConnecting()
                }
            }
        }
    }

    // Ensure session is initialized / polling when screen enters composition
    LaunchedEffect(Unit) {
        viewModel.initializePairingSession()
    }

    ScanToConnectContent(
        uiState = uiState,
        onRefreshClick = { viewModel.refreshSession() },
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the Scan to Connect UI.
 *
 * Responsiveness:
 * - Landscape (16:9): Centered branding, crisp heading and subtitle, with a prominent square QR code.
 * - Portrait (9:16): Proportional vertical stack ensuring scannability and comfortable safe display margins.
 * - Ambient background glow reflecting the Adveno brand identity.
 */
@Composable
fun ScanToConnectContent(
    uiState: ScanToConnectUiState,
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Focus requester to hold remote focus on the QR container
    val qrFocusRequester = remember { FocusRequester() }

    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        try {
            qrFocusRequester.requestFocus()
        } catch (_: Exception) {}
        entranceAlpha.animateTo(1f, animationSpec = tween(durationMillis = 350))
    }

    LaunchedEffect(Unit) {
        entranceOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 350))
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
                            Color(0x2822D3EE),
                            Color(0x143478F6),
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

        // Calculate adaptive QR code dimension while strictly maintaining 1:1 aspect ratio
        val qrSize = if (isPortrait) {
            (minOf(screenWidth * 0.62f, screenHeight * 0.38f)).coerceIn(240.dp, 360.dp)
        } else {
            (screenHeight * 0.44f).coerceIn(220.dp, 340.dp)
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

                Spacer(modifier = Modifier.height(if (isPortrait) 20.dp else 16.dp))

                // Heading: "Scan to Connect"
                Text(
                    text = uiState.heading,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AdvenoTextPrimary,
                    fontSize = if (isPortrait) 34.sp else 36.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description: "Scan this QR code from your Adveno dashboard to link this screen."
                Text(
                    text = uiState.description,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AdvenoTextSecondary,
                    fontSize = if (isPortrait) 18.sp else 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isPortrait) 20.dp else 48.dp)
                )

                Spacer(modifier = Modifier.height(if (isPortrait) 28.dp else 22.dp))

                // High-fidelity square QR Code
                if (uiState.qrMatrix != null) {
                    TvQrCode(
                        matrix = uiState.qrMatrix,
                        modifier = Modifier.size(qrSize),
                        focusRequester = qrFocusRequester
                    )
                } else {
                    TvQrCode(
                        modifier = Modifier.size(qrSize),
                        focusRequester = qrFocusRequester
                    )
                }

                // Error / Expired status handling
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    if (uiState.isExpired) {
                        Spacer(modifier = Modifier.height(14.dp))
                        TvButton(
                            text = "Refresh QR Code",
                            onClick = onRefreshClick,
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Previews for Both Orientations
// -----------------------------------------------------------------------------------------

@Preview(
    name = "Landscape - 1080p Television (16:9)",
    widthDp = 960,
    heightDp = 540,
    showBackground = true
)
@Composable
fun ScanToConnectLandscapePreview() {
    AdvenoTheme {
        ScanToConnectContent(
            uiState = ScanToConnectUiState()
        )
    }
}

@Preview(
    name = "Portrait - Vertical Signage Display (9:16)",
    widthDp = 540,
    heightDp = 960,
    showBackground = true
)
@Composable
fun ScanToConnectPortraitPreview() {
    AdvenoTheme {
        ScanToConnectContent(
            uiState = ScanToConnectUiState()
        )
    }
}
