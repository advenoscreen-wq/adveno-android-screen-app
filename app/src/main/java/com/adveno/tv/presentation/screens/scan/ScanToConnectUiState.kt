package com.adveno.tv.presentation.screens.scan

import com.adveno.tv.domain.model.pairing.PairingSessionStatus

/**
 * UI State for the Scan to Connect screen.
 * Supports QR code presentation, background polling, and network disconnection handling.
 */
data class ScanToConnectUiState(
    val brandName: String = "Adveno",
    val heading: String = "Scan to Connect",
    val description: String = "Scan this QR code from your Adveno dashboard to link this screen.",
    val qrMatrix: Array<BooleanArray>? = null,
    val isQrReady: Boolean = false,
    val isLoading: Boolean = false,
    val status: PairingSessionStatus? = null,
    val isExpired: Boolean = false,
    val errorMessage: String? = null,
    val isNoInternetModalVisible: Boolean = false,
    val interruptedOperation: String = "",
    val noInternetStatusMessage: String? = null,
    val isConnectionRestored: Boolean = false
)
