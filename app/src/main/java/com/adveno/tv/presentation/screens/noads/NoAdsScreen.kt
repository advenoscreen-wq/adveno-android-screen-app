package com.adveno.tv.presentation.screens.noads

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.R
import com.adveno.tv.presentation.components.TvButton
import com.adveno.tv.presentation.components.TvLogoutConfirmationModal
import com.adveno.tv.presentation.components.TvReportEventsErrorDialog
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoTheme

/**
 * Dedicated "No Ads Available" screen displayed when:
 * - Initial screen setup completes but no Ads are assigned.
 * - Synchronization completes but no Ads playlist is available (200 null / empty items).
 * - No locally downloaded Ads are available for playback.
 *
 * Strict 16:9 non-scrolling TV layout fitting completely inside viewport.
 * Features 'Sync Now', 'Report Events', and 'Logout' buttons with confirmation and error dialogs.
 */
@Composable
fun NoAdsScreen(
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit = {},
    isLoggingOut: Boolean = false,
    onReportEventsClick: () -> Unit = {},
    isReportingEvents: Boolean = false,
    reportSuccessMessage: String? = null,
    reportErrorMessage: String? = null,
    onRetryReport: () -> Unit = {},
    onCancelReport: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    BackHandler {
        if (showLogoutConfirmation) {
            showLogoutConfirmation = false
        } else {
            onBackClick()
        }
    }

    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffsetY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, animationSpec = tween(durationMillis = 350))
        entranceOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 350))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AdvenoBackground)
            // Atmospheric brand lighting
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x308B5CF6),
                            Color(0x144F46E5),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.2f),
                        radius = size.minDimension * 0.75f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2422D3EE),
                            Color(0x103478F6),
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

        val horizontalPadding = if (isPortrait) 28.dp else AdvenoDimensions.tvHorizontalPadding
        val verticalPadding = if (isPortrait) 32.dp else 24.dp

        // Non-scrolling, centered single-screen layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entranceAlpha.value
                        translationY = entranceOffsetY.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Adveno Logo Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_adveno_logo),
                        contentDescription = "Adveno Brand Logo",
                        modifier = Modifier.size(if (isPortrait) 44.dp else 38.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Adveno",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdvenoTextPrimary,
                        fontSize = if (isPortrait) 34.sp else 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(if (isPortrait) 28.dp else 18.dp))

                // Compact Empty-State Illustration Card
                Box(
                    modifier = Modifier
                        .size(if (isPortrait) 100.dp else 86.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AdvenoSurface)
                        .border(
                            width = 1.dp,
                            color = Color(0x3322D3EE),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isPortrait) 62.dp else 54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1822D3EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_media_image),
                            contentDescription = "No Content Assigned",
                            modifier = Modifier.size(if (isPortrait) 34.dp else 30.dp),
                            colorFilter = ColorFilter.tint(AdvenoSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isPortrait) 24.dp else 18.dp))

                // Main Heading
                Text(
                    text = "No Ads Assigned",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdvenoTextPrimary,
                    fontSize = if (isPortrait) 26.sp else 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (isPortrait) 12.dp else 8.dp))

                // Explanatory Message
                Text(
                    text = "No Ads have been assigned to this screen yet. Add Ads from your Adveno dashboard and sync to start playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdvenoTextSecondary,
                    fontSize = if (isPortrait) 15.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 580.dp)
                        .padding(horizontal = if (isPortrait) 16.dp else 40.dp)
                )

                Spacer(modifier = Modifier.height(if (isPortrait) 28.dp else 22.dp))

                // Action Buttons: Sync Now, Report Events, & Logout
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvButton(
                        text = "Sync Now",
                        onClick = onSyncClick,
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(AdvenoTextPrimary),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.widthIn(min = 160.dp)
                    )

                    TvButton(
                        text = if (isReportingEvents) "Reporting..." else "Report Events",
                        onClick = onReportEventsClick,
                        isLoading = isReportingEvents,
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cloud_upload),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(AdvenoTextPrimary),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.widthIn(min = 175.dp)
                    )

                    TvButton(
                        text = if (isLoggingOut) "Logging out..." else "Logout",
                        onClick = { showLogoutConfirmation = true },
                        isLoading = isLoggingOut,
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logout),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(AdvenoTextPrimary),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.widthIn(min = 160.dp)
                    )
                }
            }

            // Floating Success Notification Banner
            AnimatedVisibility(
                visible = reportSuccessMessage != null,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -20 },
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -20 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isPortrait) 16.dp else 20.dp)
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
                        text = reportSuccessMessage ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Logout Confirmation Dialog
        TvLogoutConfirmationModal(
            isVisible = showLogoutConfirmation,
            isLoggingOut = isLoggingOut,
            onConfirm = {
                showLogoutConfirmation = false
                onLogoutClick()
            },
            onDismiss = {
                showLogoutConfirmation = false
            }
        )

        // Playback Event Reporting Error Dialog
        TvReportEventsErrorDialog(
            isVisible = reportErrorMessage != null,
            errorMessage = reportErrorMessage,
            onRetry = onRetryReport,
            onCancel = onCancelReport
        )
    }
}

@Preview(name = "Landscape - No Ads 1080p", widthDp = 960, heightDp = 540, showBackground = true)
@Composable
fun NoAdsLandscapePreview() {
    AdvenoTheme {
        NoAdsScreen(onSyncClick = {}, onLogoutClick = {})
    }
}

@Preview(name = "Portrait - No Ads", widthDp = 540, heightDp = 960, showBackground = true)
@Composable
fun NoAdsPortraitPreview() {
    AdvenoTheme {
        NoAdsScreen(onSyncClick = {}, onLogoutClick = {})
    }
}
