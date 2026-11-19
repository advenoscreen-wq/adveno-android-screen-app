package com.adveno.tv.presentation.screens.connect

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.usecase.pairing.CreatePairingSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Connect Your Screen.
 * Initiates the pairing session via backend API and navigates to QR code display upon success.
 */
class ConnectViewModel(
    private val createPairingSessionUseCase: CreatePairingSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    fun onNewScreenSelected(onNavigate: () -> Unit) {
        createSession(
            type = PairingType.NEW_SCREEN,
            action = ConnectAction.NewScreen,
            onSuccess = onNavigate
        )
    }

    fun onReconnectSelected(onNavigate: () -> Unit) {
        createSession(
            type = PairingType.RECONNECT_EXISTING,
            action = ConnectAction.Reconnect,
            onSuccess = onNavigate
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun createSession(
        type: PairingType,
        action: ConnectAction,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isLoading) return

        Log.d("ConnectViewModel", "Creating pairing session for type: $type")
        _uiState.update {
            it.copy(
                isLoading = true,
                selectedAction = action,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            createPairingSessionUseCase(type).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        Log.d("ConnectViewModel", "Pairing session successfully created: ${result.data.sessionId}")
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess()
                    }
                    is NetworkResult.Error -> {
                        val message = result.error.message.takeIf { !it.isNullOrBlank() }
                            ?: "Failed to create pairing session. Please check your network and try again."
                        Log.e("ConnectViewModel", "Failed to create pairing session: $message")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = message
                            )
                        }
                    }
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
}

