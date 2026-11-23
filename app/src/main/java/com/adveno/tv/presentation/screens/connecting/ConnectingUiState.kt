package com.adveno.tv.presentation.screens.connecting

/**
 * UI State for the Pairing / Connecting screen.
 * Covers active connecting loading phase, 3-attempt retry loop,
 * network disconnection modal state, and the "Connected!" success state.
 */
data class ConnectingUiState(
    val brandName: String = "Adveno",
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val currentAttempt: Int = 1,
    val maxAttempts: Int = 3,
    val isMaxAttemptsReached: Boolean = false,
    val heading: String = if (isConnected) "Connected!" else "Connecting...",
    val description: String = if (isConnected) {
        "Your screen is now connected to Adveno."
    } else {
        "Linking this screen to your Adveno account"
    },
    val errorMessage: String? = null,
    val isNoInternetModalVisible: Boolean = false,
    val interruptedOperation: String = "",
    val noInternetStatusMessage: String? = null,
    val isConnectionRestored: Boolean = false
) {
    val attemptIndicatorText: String
        get() = "Attempt $currentAttempt of $maxAttempts"
}
