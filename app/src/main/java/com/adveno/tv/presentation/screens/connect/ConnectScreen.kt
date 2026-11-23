package com.adveno.tv.presentation.screens.connect

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.R
import com.adveno.tv.presentation.components.TvButton
import com.adveno.tv.presentation.components.TvNoInternetModal
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Stateful entry point for the "Connect Your Screen" screen.
 */
@Composable
fun ConnectScreen(
    viewModel: ConnectViewModel,
    onBackClick: () -> Unit,
    onNavigateToNewScreen: () -> Unit,
    onNavigateToReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle Android TV remote Back button to return to Start Connecting
    BackHandler {
        onBackClick()
    }

    ConnectScreenContent(
        uiState = uiState,
        onNewScreenClick = {
            viewModel.onNewScreenSelected(onNavigate = onNavigateToNewScreen)
        },
        onReconnectClick = {
            viewModel.onReconnectSelected(onNavigate = onNavigateToReconnect)
        },
        onRetryConnection = {
            viewModel.retryConnectionFromModal()
        },
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the Connect Your Screen UI.
 *
 * Responsive layout:
 * - Landscape (16:9): Horizontal side-by-side buttons with D-pad LEFT/RIGHT navigation.
 * - Portrait (9:16): Vertical stacked buttons with D-pad UP/DOWN navigation.
 */
@Composable
fun ConnectScreenContent(
    uiState: ConnectUiState,
    onNewScreenClick: () -> Unit,
    onReconnectClick: () -> Unit,
    onRetryConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Initial focus requester for the primary action button (New Screen)
    val newScreenFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        newScreenFocusRequester.requestFocus()
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
        val verticalPadding = if (isPortrait) 44.dp else AdvenoDimensions.tvVerticalPadding

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = uiState.brandName,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdvenoTextPrimary,
                        fontSize = 46.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Main Heading
                Text(
                    text = uiState.heading,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AdvenoTextPrimary,
                    fontSize = if (isPortrait) 36.sp else 40.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle
                Text(
                    text = uiState.subtitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AdvenoTextSecondary,
                    fontSize = if (isPortrait) 18.sp else 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                val buttonSpacerHeight = if (isPortrait) {
                    (screenHeight * 0.08f).coerceIn(36.dp, 64.dp)
                } else {
                    (screenHeight * 0.08f).coerceIn(36.dp, 56.dp)
                }

                Spacer(modifier = Modifier.height(buttonSpacerHeight))

                // Error message display if session creation fails
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Responsive Action Buttons
                if (isPortrait) {
                    // Portrait (9:16): Vertical Stack (UP / DOWN navigation)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TvButton(
                            text = uiState.newScreenButtonText,
                            onClick = onNewScreenClick,
                            isLoading = uiState.isLoading && uiState.selectedAction == ConnectAction.NewScreen,
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_box),
                                    contentDescription = "New Screen Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .widthIn(min = 280.dp, max = 400.dp)
                                .focusRequester(newScreenFocusRequester)
                        )

                        TvButton(
                            text = uiState.reconnectButtonText,
                            onClick = onReconnectClick,
                            isLoading = uiState.isLoading && uiState.selectedAction == ConnectAction.Reconnect,
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Reconnect Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .widthIn(min = 280.dp, max = 400.dp)
                        )
                    }
                } else {
                    // Landscape (16:9): Horizontal Side-by-Side (LEFT / RIGHT navigation)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvButton(
                            text = uiState.newScreenButtonText,
                            onClick = onNewScreenClick,
                            isLoading = uiState.isLoading && uiState.selectedAction == ConnectAction.NewScreen,
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_box),
                                    contentDescription = "New Screen Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier
                                .width(260.dp)
                                .focusRequester(newScreenFocusRequester)
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        TvButton(
                            text = uiState.reconnectButtonText,
                            onClick = onReconnectClick,
                            isLoading = uiState.isLoading && uiState.selectedAction == ConnectAction.Reconnect,
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Reconnect Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }
            }
        }

        // Common No Internet Connection Modal overlay
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
// Previews for Both Orientations
// -----------------------------------------------------------------------------------------

@Preview(
    name = "Connect Screen - Landscape 1080p (16:9)",
    widthDp = 960,
    heightDp = 540,
    showBackground = true
)
@Composable
fun ConnectScreenLandscapePreview() {
    AdvenoTheme {
        ConnectScreenContent(
            uiState = ConnectUiState(),
            onNewScreenClick = {},
            onReconnectClick = {}
        )
    }
}

@Preview(
    name = "Connect Screen - Portrait Signage (9:16)",
    widthDp = 540,
    heightDp = 960,
    showBackground = true
)
@Composable
fun ConnectScreenPortraitPreview() {
    AdvenoTheme {
        ConnectScreenContent(
            uiState = ConnectUiState(),
            onNewScreenClick = {},
            onReconnectClick = {}
        )
    }
}

