package com.adveno.tv.presentation.screens.connect

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkExceptionClassifier
import com.adveno.tv.core.network.NetworkMonitor
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
 * Fully network-aware: pauses and displays the common No Internet modal without exposing raw DNS errors.
 */
class ConnectViewModel(
    private val createPairingSessionUseCase: CreatePairingSessionUseCase,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private var pendingType: PairingType? = null
    private var pendingAction: ConnectAction? = null
    private var pendingOnSuccess: (() -> Unit)? = null

    init {
        observeNetworkConnectivity()
    }

    private fun observeNetworkConnectivity() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isConnectedFlow.collect { isConnected ->
                if (_uiState.value.isNoInternetModalVisible) {
                    _uiState.update {
                        it.copy(
                            isConnectionRestored = isConnected,
                            noInternetStatusMessage = if (isConnected) {
                                "Internet connection restored. Press Retry Connection to continue."
                            } else {
                                "Internet is still unavailable. Please check your network or Wi-Fi settings."
                            }
                        )
                    }
                }
            }
        }
    }

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

        // Preserve pending parameters for resumability
        pendingType = type
        pendingAction = action
        pendingOnSuccess = onSuccess

        // 1. Pre-check: Check device internet before network request
        if (networkMonitor?.isCurrentlyConnected() == false) {
            Log.w("ConnectViewModel", "No internet connection before session creation. Pausing workflow.")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedAction = action,
                    isNoInternetModalVisible = true,
                    interruptedOperation = if (action == ConnectAction.NewScreen) "New Screen Connection" else "Screen Reconnection",
                    noInternetStatusMessage = null,
                    isConnectionRestored = false,
                    errorMessage = null
                )
            }
            return
        }

        Log.d("ConnectViewModel", "Creating pairing session for type: $type")
        _uiState.update {
            it.copy(
                isLoading = true,
                selectedAction = action,
                errorMessage = null,
                isNoInternetModalVisible = false
            )
        }

        viewModelScope.launch {
            createPairingSessionUseCase(type).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        Log.d("ConnectViewModel", "Pairing session successfully created: ${result.data.sessionId}")
                        _uiState.update { it.copy(isLoading = false, isNoInternetModalVisible = false) }
                        pendingType = null
                        pendingAction = null
                        pendingOnSuccess = null
                        onSuccess()
                    }
                    is NetworkResult.Error -> {
                        val error = result.error
                        val isNetworkLoss = error.isNetworkConnectionIssue ||
                                networkMonitor?.isCurrentlyConnected() == false

                        if (isNetworkLoss) {
                            Log.w("ConnectViewModel", "Network error during pairing session creation. Showing No Internet modal.")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isNoInternetModalVisible = true,
                                    interruptedOperation = if (action == ConnectAction.NewScreen) "New Screen Connection" else "Screen Reconnection",
                                    noInternetStatusMessage = null,
                                    isConnectionRestored = false,
                                    errorMessage = null
                                )
                            }
                        } else {
                            val safeMessage = "Unable to create pairing session. Please try again."
                            Log.e("ConnectViewModel", "Failed to create pairing session: ${error.message}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = safeMessage
                                )
                            }
                        }
                    }
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Resumes the preserved session creation operation when internet connectivity returns.
     */
    fun retryConnectionFromModal() {
        if (networkMonitor?.isCurrentlyConnected() == false) {
            _uiState.update {
                it.copy(
                    noInternetStatusMessage = "Internet is still unavailable. Please check your network or Wi-Fi settings.",
                    isConnectionRestored = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isNoInternetModalVisible = false,
                noInternetStatusMessage = null,
                isConnectionRestored = false
            )
        }

        val type = pendingType ?: PairingType.NEW_SCREEN
        val action = pendingAction ?: ConnectAction.NewScreen
        val onSuccess = pendingOnSuccess ?: {}
        createSession(type, action, onSuccess)
    }
}

