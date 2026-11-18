package com.adveno.tv.presentation.screens.home

import com.adveno.tv.domain.model.ConnectionStatus

/**
 * UI State for the Home screen, modeling Unidirectional Data Flow (UDF).
 * The UI simply renders this immutable state.
 */
data class HomeUiState(
    val title: String = "Adveno",
    val subtitle: String = "Welcome to Adveno",
    val buttonText: String = "Start Connecting",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val isConnecting: Boolean = false
)

