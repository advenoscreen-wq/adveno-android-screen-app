package com.adveno.tv.presentation.screens.home

/**
 * UI State for the Start Connecting / Home screen.
 * Models Unidirectional Data Flow (UDF).
 */
data class HomeUiState(
    val title: String = "Adveno",
    val subtitle: String = "Digital Signage & Screen Management",
    val buttonText: String = "Start Connecting",
    val isConnecting: Boolean = false,
    val isCredentialValid: Boolean = false
)
