package com.adveno.tv.presentation.screens.connecting

/**
 * UI State for the Pairing / Connecting screen.
 * Covers both the active "Connecting..." loading phase and the "Connected!" success state on the same screen.
 */
data class ConnectingUiState(
    val brandName: String = "Adveno",
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val heading: String = if (isConnected) "Connected!" else "Connecting...",
    val description: String = if (isConnected) {
        "Your screen is now connected to Adveno."
    } else {
        "Linking this screen to your Adveno account"
    },
    val errorMessage: String? = null
)
