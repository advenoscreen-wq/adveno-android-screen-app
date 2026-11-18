package com.adveno.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.domain.model.ConnectionStatus
import com.adveno.tv.presentation.components.TvButton
import com.adveno.tv.presentation.components.TvStatusBadge
import com.adveno.tv.presentation.theme.AdvenoBackground
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoTextMuted
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import com.adveno.tv.presentation.theme.AdvenoWarning

/**
 * Stateful entry point for HomeScreen interacting with HomeViewModel.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        uiState = uiState,
        onStartConnectingClick = viewModel::onStartConnecting,
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the Adveno HomeScreen UI.
 * Follows the 10-foot TV viewing experience with safe margins and generous typography.
 */
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onStartConnectingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AdvenoBackground)
            .padding(
                horizontal = AdvenoDimensions.tvHorizontalPadding,
                vertical = AdvenoDimensions.tvVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        // Status indicator placed in top-right corner of safe TV area
        val (badgeLabel, badgeColor) = when (uiState.connectionStatus) {
            is ConnectionStatus.Connected -> "Screen Online" to AdvenoSuccess
            is ConnectionStatus.Connecting -> "Connecting..." to AdvenoWarning
            is ConnectionStatus.Disconnected -> "Screen Offline" to AdvenoTextMuted
            is ConnectionStatus.Error -> "Connection Error" to MaterialTheme.colorScheme.error
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            TvStatusBadge(
                statusText = badgeLabel,
                statusColor = badgeColor
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = AdvenoDimensions.spaceXl)
        ) {
            // App Title
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = AdvenoTextPrimary,
                fontSize = 54.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AdvenoDimensions.spaceMd))

            // Subtitle
            Text(
                text = uiState.subtitle,
                style = MaterialTheme.typography.headlineMedium,
                color = AdvenoTextSecondary,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AdvenoDimensions.spaceHero))

            // Premium TV Remote Focusable Action Button
            TvButton(
                text = uiState.buttonText,
                onClick = onStartConnectingClick,
                enabled = !uiState.isConnecting,
                isLoading = uiState.isConnecting
            )
        }
    }
}
