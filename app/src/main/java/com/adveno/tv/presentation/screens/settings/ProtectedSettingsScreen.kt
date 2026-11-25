package com.adveno.tv.presentation.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.R
import com.adveno.tv.presentation.components.TvLogoutConfirmationModal
import com.adveno.tv.presentation.components.TvNoInternetModal
import com.adveno.tv.presentation.components.TvReportEventsErrorDialog
import com.adveno.tv.presentation.components.TvSignageControlPill
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoButtonFocused
import com.adveno.tv.presentation.theme.AdvenoButtonUnfocused
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoError
import com.adveno.tv.presentation.theme.AdvenoFocusBorder
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoWarning

private enum class SettingsSection {
    ACTIONS,
    CONNECTION_INFO,
    SCREEN_INFO
}

/**
 * Protected Settings Screen for Android TV.
 *
 * Implements:
 * - 16:9 non-scrolling TV layout.
 * - Strict Adveno dark theme (#101522 background, #1B2435 cards, #3478F6 accent, #22D3EE cyan).
 * - Full Android TV remote D-pad navigation.
 * - 5 administrative actions:
 *   1. Sync Ads (manual synchronization)
 *   2. Playback Reports (manually report playback history)
 *   3. Connection Info
 *   4. Screen Info
 *   5. Logout / Unpair (with confirmation dialog)
 * - Automatic session expiration on 5-min inactivity or 15-min limit.
 * - Clear exit back to Ads playback, securely destroying the temporary session token.
 */
@Composable
fun ProtectedSettingsScreen(
    viewModel: ProtectedSettingsViewModel,
    onExit: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToStartConnecting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(SettingsSection.ACTIONS) }

    // Remote back button securely clears session and exits
    BackHandler(enabled = !uiState.isLoggingOut) {
        if (showLogoutConfirmation) {
            showLogoutConfirmation = false
        } else {
            viewModel.exitSettings(onExit)
        }
    }

    // React to backend session expiration / inactivity
    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            viewModel.exitSettings(onExit)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AdvenoBackground)
            .onKeyEvent {
                viewModel.onUserInteraction()
                false
            }
    ) {
        val isPortrait = maxHeight > maxWidth

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isPortrait) 24.dp else AdvenoDimensions.tvHorizontalPadding,
                    vertical = if (isPortrait) 24.dp else 22.dp
                )
        ) {
            // -------------------------------------------------------------
            // TOP BAR: Brand Logo, Protected Settings Badge, and Exit Pill
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_adveno_logo),
                        contentDescription = "Adveno Brand Logo",
                        modifier = Modifier.size(32.dp)
                    )

                    Column {
                        Text(
                            text = "Protected Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AdvenoTextPrimary,
                            fontSize = 20.sp
                        )

                        Text(
                            text = "Admin Session Active",
                            color = AdvenoSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Exit / Return to Ads button
                TvSignageControlPill(
                    text = "Exit Settings",
                    iconRes = R.drawable.ic_logout,
                    onClick = { viewModel.exitSettings(onExit) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -------------------------------------------------------------
            // MAIN CONTENT: 2-Panel Layout (Left: Action Cards, Right: Details Card)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT PANEL: 5 Main Administrative Actions
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Sync Ads Action Card
                    TvSettingsActionCard(
                        title = "Sync Ads",
                        description = "Fetch latest playlists and download new media",
                        iconRes = R.drawable.ic_refresh,
                        accentColor = AdvenoPrimary,
                        onClick = {
                            viewModel.onUserInteraction()
                            onNavigateToSync()
                        }
                    )

                    // 2. Playback Reports Action Card
                    TvSettingsActionCard(
                        title = "Playback Reports",
                        description = if (uiState.pendingEventsCount > 0)
                            "${uiState.pendingEventsCount} pending event(s) waiting to upload"
                        else
                            "All playback records are synchronized",
                        iconRes = R.drawable.ic_cloud_upload,
                        accentColor = AdvenoSecondary,
                        isLoading = uiState.isReportingEvents,
                        badgeText = if (uiState.pendingEventsCount > 0) "${uiState.pendingEventsCount}" else null,
                        onClick = {
                            viewModel.reportPlaybackEvents()
                        }
                    )

                    // 3. Connection Info Action Card
                    TvSettingsActionCard(
                        title = "Connection Info",
                        description = if (uiState.isOnline) "Connected to Adveno Network" else "Device is Offline",
                        iconRes = R.drawable.ic_wifi_off,
                        accentColor = if (uiState.isOnline) AdvenoSuccess else AdvenoWarning,
                        isSelected = selectedSection == SettingsSection.CONNECTION_INFO,
                        onClick = {
                            viewModel.onUserInteraction()
                            selectedSection = SettingsSection.CONNECTION_INFO
                        }
                    )

                    // 4. Screen Info Action Card
                    TvSettingsActionCard(
                        title = "Screen Info",
                        description = "ID: ${uiState.screenId.take(16)}...",
                        iconRes = R.drawable.ic_link,
                        accentColor = Color(0xFFA855F7),
                        isSelected = selectedSection == SettingsSection.SCREEN_INFO,
                        onClick = {
                            viewModel.onUserInteraction()
                            selectedSection = SettingsSection.SCREEN_INFO
                        }
                    )

                    // 5. Logout / Unpair Action Card
                    TvSettingsActionCard(
                        title = "Logout / Unpair",
                        description = "Disconnect screen and revoke local credential",
                        iconRes = R.drawable.ic_logout,
                        accentColor = AdvenoError,
                        isDestructive = true,
                        isLoading = uiState.isLoggingOut,
                        onClick = {
                            viewModel.onUserInteraction()
                            showLogoutConfirmation = true
                        }
                    )
                }

                // RIGHT PANEL: Status & Information Viewer Card
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AdvenoSurface)
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    when (selectedSection) {
                        SettingsSection.CONNECTION_INFO -> {
                            ConnectionInfoContent(uiState = uiState)
                        }
                        SettingsSection.SCREEN_INFO -> {
                            ScreenInfoContent(uiState = uiState)
                        }
                        SettingsSection.ACTIONS -> {
                            LiveStatusOverviewContent(uiState = uiState)
                        }
                    }
                }
            }

            // Floating Success Notification Banner
            AnimatedVisibility(
                visible = uiState.reportSuccessMessage != null,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -20 },
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -20 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .shadow(12.dp, shape = RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE60F172A))
                        .border(1.5.dp, AdvenoSuccess, RoundedCornerShape(20.dp))
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

        // Logout Confirmation Dialog
        TvLogoutConfirmationModal(
            isVisible = showLogoutConfirmation,
            isLoggingOut = uiState.isLoggingOut,
            onConfirm = {
                showLogoutConfirmation = false
                viewModel.logout(
                    onSuccess = {
                        onNavigateToStartConnecting()
                    }
                )
            },
            onDismiss = {
                showLogoutConfirmation = false
            }
        )

        // No Internet Modal
        TvNoInternetModal(
            isVisible = uiState.isNoInternetModalVisible,
            interruptedOperation = when (uiState.pendingAction) {
                ProtectedAction.REPORT_EVENTS -> "Reporting Playback Events"
                ProtectedAction.LOGOUT -> "Logging Out Screen"
                ProtectedAction.SYNC_ADS, null -> "Protected Network Request"
            },
            onRetryConnection = {
                viewModel.retryPendingNetworkAction(
                    onLogoutSuccess = {
                        onNavigateToStartConnecting()
                    }
                )
            }
        )

        // Reporting Error Dialog
        TvReportEventsErrorDialog(
            isVisible = uiState.reportErrorMessage != null,
            errorMessage = uiState.reportErrorMessage,
            onRetry = {
                viewModel.reportPlaybackEvents()
            },
            onCancel = {
                viewModel.dismissReportError()
            }
        )
    }
}

/**
 * TV D-pad focusable administrative Action Card.
 */
@Composable
private fun TvSettingsActionCard(
    title: String,
    description: String,
    iconRes: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    badgeText: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "ActionCardScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> AdvenoButtonFocused
            isSelected -> Color(0x2E3478F6)
            isDestructive -> Color(0x14EF4444)
            else -> AdvenoSurface
        },
        animationSpec = tween(durationMillis = 150),
        label = "ActionCardBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> AdvenoFocusBorder
            isSelected -> AdvenoPrimary
            isDestructive -> Color(0x33EF4444)
            else -> Color(0x14FFFFFF)
        },
        animationSpec = tween(durationMillis = 150),
        label = "ActionCardBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading,
                onClick = onClick
            )
            .focusable(enabled = !isLoading, interactionSource = interactionSource)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = accentColor,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdvenoTextPrimary,
                    fontSize = 15.sp
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AdvenoTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LiveStatusOverviewContent(uiState: ProtectedSettingsUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Status Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AdvenoTextPrimary,
            fontSize = 18.sp
        )

        InfoDataRow(label = "Network Status", value = if (uiState.isOnline) "Connected" else "Offline", isHighlight = uiState.isOnline)
        InfoDataRow(label = "Active Playlist", value = uiState.playlistName)
        InfoDataRow(label = "Playlist Version", value = "v${uiState.activeRevision}")
        InfoDataRow(label = "Media Assets", value = "${uiState.playlistItemCount} items")
        InfoDataRow(label = "Pending Events", value = "${uiState.pendingEventsCount} records")
        InfoDataRow(label = "Screen ID", value = uiState.screenId)
    }
}

@Composable
private fun ConnectionInfoContent(uiState: ProtectedSettingsUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Connection & Network",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AdvenoTextPrimary,
            fontSize = 18.sp
        )

        InfoDataRow(label = "Server Base URL", value = uiState.baseUrl)
        InfoDataRow(label = "Internet Capability", value = if (uiState.isOnline) "Validated" else "Disconnected", isHighlight = uiState.isOnline)
        InfoDataRow(label = "Sync Protocol", value = "HTTP/2 REST + ngrok tunnel")
        InfoDataRow(label = "Security", value = "Encrypted Keystore + Bearer Token")
        InfoDataRow(label = "Admin Session Header", value = "X-Admin-Session")
    }
}

@Composable
private fun ScreenInfoContent(uiState: ProtectedSettingsUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Device & Screen Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AdvenoTextPrimary,
            fontSize = 18.sp
        )

        InfoDataRow(label = "Hardware Model", value = uiState.deviceModel)
        InfoDataRow(label = "OS Platform", value = uiState.androidVersion)
        InfoDataRow(label = "Application Version", value = uiState.appVersion)
        InfoDataRow(label = "Screen ID", value = uiState.screenId)
        InfoDataRow(label = "Display Mode", value = "16:9 Landscape Digital Signage")
    }
}

@Composable
private fun InfoDataRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AdvenoTextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlight) AdvenoSuccess else AdvenoTextPrimary,
            fontSize = 14.sp
        )
    }
}
