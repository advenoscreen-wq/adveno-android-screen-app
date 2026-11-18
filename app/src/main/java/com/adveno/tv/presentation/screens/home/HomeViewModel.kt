package com.adveno.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.data.repository.ConnectionRepositoryImpl
import com.adveno.tv.domain.model.ConnectionStatus
import com.adveno.tv.domain.usecase.ConnectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Start Connecting screen.
 * Handles user interaction events and prepares for the pairing / connection flow.
 */
class HomeViewModel(
    private val connectUseCase: ConnectUseCase = ConnectUseCase(ConnectionRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Triggered when the user clicks or presses Enter on "Start Connection".
     * Prepares the transition to the upcoming New Screen / Reconnect selection flow.
     */
    fun onStartConnecting(onNavigateToPairing: (() -> Unit)? = null) {
        if (_uiState.value.isConnecting) return

        Log.d("HomeViewModel", "Start Connection triggered from Android TV remote")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnecting = true,
                    buttonText = "Connecting..."
                )
            }

            connectUseCase()

            _uiState.update {
                it.copy(
                    isConnecting = false,
                    connectionStatus = ConnectionStatus.Connected,
                    buttonText = "Start Connecting",
                    isReadyToPair = true
                )
            }

            // Placeholder hook for upcoming New Screen / Reconnect navigation flow
            onNavigateToPairing?.invoke()
        }
    }
}
