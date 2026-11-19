package com.adveno.tv.presentation.screens.home

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
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Stateful entry point for the Start Connecting / Home screen.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPairing: () -> Unit,
    onNavigateToAds: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Re-check and verify credentials whenever the Home screen is displayed
    LaunchedEffect(Unit) {
        viewModel.checkAndVerifyCredential()
    }

    HomeScreenContent(
        uiState = uiState,
        onActionButtonClick = {
            viewModel.onActionButtonClick(
                onNavigateToPairing = onNavigateToPairing,
                onNavigateToAds = onNavigateToAds
            )
        },
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the Adveno Start Connecting UI.
 *
 * Fully responsive:
 * - Landscape (16:9 standard TV): Horizontal branding layout with balanced vertical breathing room.
 * - Portrait (9:16 vertical signage): Vertical stack with proportional spacing and wide adaptive button.
 * - Ambient glow background reflecting the premium digital signage brand identity.
 */
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonFocusRequester = remember { FocusRequester() }

    // Automatically request initial D-pad focus on screen launch
    LaunchedEffect(Unit) {
        buttonFocusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AdvenoBackground)
            // Atmospheric ambient lighting gradients matching the Adveno brand identity
            .drawBehind {
                // Top-left purple/indigo ambient glow
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
                // Bottom-right cyan/indigo ambient glow
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
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // Responsive Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isPortrait) {
                    // Portrait (9:16) Brand Presentation
                    PortraitBranding(
                        title = uiState.title,
                        subtitle = uiState.subtitle
                    )
                } else {
                    // Landscape (16:9) Brand Presentation
                    LandscapeBranding(
                        title = uiState.title,
                        subtitle = uiState.subtitle
                    )
                }

                // Adaptive spacing based on available viewport height
                val buttonSpacerHeight = if (isPortrait) {
                    (screenHeight * 0.08f).coerceIn(36.dp, 72.dp)
                } else {
                    (screenHeight * 0.07f).coerceIn(32.dp, 56.dp)
                }

                Spacer(modifier = Modifier.height(buttonSpacerHeight))

                // TV Remote Focusable Action Button
                val buttonModifier = if (isPortrait) {
                    Modifier
                        .fillMaxWidth(0.85f)
                        .widthIn(min = 280.dp, max = 420.dp)
                        .focusRequester(buttonFocusRequester)
                } else {
                    Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .focusRequester(buttonFocusRequester)
                }

                TvButton(
                    text = uiState.buttonText,
                    onClick = onActionButtonClick,
                    enabled = !uiState.isConnecting,
                    isLoading = uiState.isConnecting,
                    leadingIcon = {
                        val iconRes = if (uiState.isCredentialValid) {
                            R.drawable.ic_media_play
                        } else {
                            R.drawable.ic_link
                        }
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = uiState.buttonText,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    modifier = buttonModifier
                )
            }
        }
    }
}

/**
 * Landscape (16:9) Branding layout:
 * Horizontal logo + title row, followed by elegant subtitle.
 */
@Composable
private fun LandscapeBranding(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_adveno_logo),
                contentDescription = "Adveno Brand Logo",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = AdvenoTextPrimary,
                fontSize = 54.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.headlineMedium,
            color = AdvenoTextSecondary,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Portrait (9:16) Branding layout:
 * Centered vertical stack with prominent logo, bold title, and balanced subtitle.
 */
@Composable
private fun PortraitBranding(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_adveno_logo),
            contentDescription = "Adveno Brand Logo",
            modifier = Modifier.size(88.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
            color = AdvenoTextPrimary,
            fontSize = 46.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.headlineMedium,
            color = AdvenoTextSecondary,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center
        )
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
fun HomeScreenLandscapePreview() {
    AdvenoTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onActionButtonClick = {}
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
fun HomeScreenPortraitPreview() {
    AdvenoTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onActionButtonClick = {}
        )
    }
}
