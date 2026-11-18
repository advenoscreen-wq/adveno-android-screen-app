package com.adveno.tv.presentation.screens.scan

/**
 * UI State for the Scan to Connect screen.
 */
data class ScanToConnectUiState(
    val brandName: String = "Adveno",
    val heading: String = "Scan to Connect",
    val description: String = "Scan this QR code from your Adveno dashboard to link this screen.",
    val isQrReady: Boolean = true
)

