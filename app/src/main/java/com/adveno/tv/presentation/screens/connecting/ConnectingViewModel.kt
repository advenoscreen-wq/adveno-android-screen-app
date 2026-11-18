package com.adveno.tv.presentation.screens.connecting

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the Pairing / Connecting screen.
 * Coordinates the state transition between the initial connecting rotation and the final connected state.
 */
class ConnectingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectingUiState())
    val uiState: StateFlow<ConnectingUiState> = _uiState.asStateFlow()

    fun markConnected() {
        _uiState.update {
            it.copy(
                isConnected = true,
                heading = "Connected!",
                description = "Your screen is now connected to Adveno."
            )
        }
    }

    fun reset() {
        _uiState.value = ConnectingUiState(
            isConnected = false,
            heading = "Connecting...",
            description = "Linking this screen to your Adveno account"
        )
    }
}
