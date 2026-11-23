package com.adveno.tv.presentation.screens.connect

/**
 * UI State for the "Connect Your Screen" selection screen.
 * Follows Unidirectional Data Flow (UDF).
 */
data class ConnectUiState(
    val brandName: String = "Adveno",
    val heading: String = "Connect Your Screen",
    val subtitle: String = "Set up a new screen or reconnect an existing one.",
    val newScreenButtonText: String = "New Screen",
    val reconnectButtonText: String = "Reconnect",
    val selectedAction: ConnectAction? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNoInternetModalVisible: Boolean = false,
    val interruptedOperation: String = "",
    val noInternetStatusMessage: String? = null,
    val isConnectionRestored: Boolean = false
)

sealed interface ConnectAction {
    data object NewScreen : ConnectAction
    data object Reconnect : ConnectAction
}

