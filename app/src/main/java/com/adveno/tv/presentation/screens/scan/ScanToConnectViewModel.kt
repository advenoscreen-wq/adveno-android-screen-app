package com.adveno.tv.presentation.screens.scan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Scan to Connect.
 * Manages the UI state for the QR pairing display.
 * When real pairing backend is integrated, this ViewModel will observe pairing token events
 * and registration statuses without requiring changes to the screen layout.
 */
class ScanToConnectViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanToConnectUiState())
    val uiState: StateFlow<ScanToConnectUiState> = _uiState.asStateFlow()
}
