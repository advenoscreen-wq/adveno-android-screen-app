package com.adveno.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkMonitor
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.usecase.screen.VerifyCredentialUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Start Connecting screen.
 * Checks for existing local screen credentials on startup and verifies them with the backend.
 * Network-aware: pauses with the common No Internet modal if offline during verification.
 */
class HomeViewModel(
    private val verifyCredentialUseCase: VerifyCredentialUseCase,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeNetworkConnectivity()
        checkAndVerifyCredential()
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

    /**
     * Checks if a local credential exists, and if so, verifies it via POST /api/v1/screens/credential/verify.
     */
    fun checkAndVerifyCredential() {
        if (!verifyCredentialUseCase.hasActiveCredential()) {
            Log.d("HomeViewModel", "No local credential found. Setting button to Start Connecting.")
            _uiState.update {
                it.copy(
                    buttonText = "Start Connecting",
                    isConnecting = false,
                    isCredentialValid = false,
                    isNoInternetModalVisible = false
                )
            }
            return
        }

        // Check network before remote verification
        if (networkMonitor?.isCurrentlyConnected() == false) {
            Log.w("HomeViewModel", "No internet connection for credential verification. Pausing.")
            _uiState.update {
                it.copy(
                    buttonText = "Connecting...",
                    isConnecting = false,
                    isNoInternetModalVisible = true,
                    interruptedOperation = "Device Authentication",
                    noInternetStatusMessage = null,
                    isConnectionRestored = false
                )
            }
            return
        }

        Log.d("HomeViewModel", "Local credential found. Verifying with backend...")
        _uiState.update {
            it.copy(
                buttonText = "Connecting...",
                isConnecting = true,
                isCredentialValid = false,
                isNoInternetModalVisible = false
            )
        }

        viewModelScope.launch {
            verifyCredentialUseCase().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val isValid = result.data
                        Log.d("HomeViewModel", "Credential verification result: valid=$isValid")
                        if (isValid) {
                            _uiState.update {
                                it.copy(
                                    buttonText = "Start Ads",
                                    isConnecting = false,
                                    isCredentialValid = true,
                                    isNoInternetModalVisible = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    buttonText = "Start Connecting",
                                    isConnecting = false,
                                    isCredentialValid = false,
                                    isNoInternetModalVisible = false
                                )
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        val error = result.error
                        val isServerAuthError = error is NetworkError.DeviceRevoked ||
                                (error is NetworkError.HttpError && (error.httpCode == 401 || error.httpCode == 404))

                        val isNetworkLoss = !isServerAuthError &&
                                (error.isNetworkConnectionIssue || networkMonitor?.isCurrentlyConnected() == false)

                        if (isNetworkLoss) {
                            Log.w("HomeViewModel", "Network error during credential verification. Showing No Internet modal.")
                            _uiState.update {
                                it.copy(
                                    isConnecting = false,
                                    isNoInternetModalVisible = true,
                                    interruptedOperation = "Device Authentication",
                                    noInternetStatusMessage = null,
                                    isConnectionRestored = false
                                )
                            }
                        } else {
                            Log.w("HomeViewModel", "Credential verification failed: ${error.message}")
                            _uiState.update {
                                it.copy(
                                    buttonText = "Start Connecting",
                                    isConnecting = false,
                                    isCredentialValid = false,
                                    isNoInternetModalVisible = false
                                )
                            }
                        }
                    }
                    is NetworkResult.Loading -> {
                        _uiState.update {
                            it.copy(
                                buttonText = "Connecting...",
                                isConnecting = true
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Resumes credential verification when internet connection is restored.
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

        checkAndVerifyCredential()
    }

    /**
     * Handles action button click:
     * - If credential is valid: navigates to Ads Running screen.
     * - If credential is not valid or missing: navigates to Pairing/Connect screen.
     */
    fun onActionButtonClick(
        onNavigateToPairing: () -> Unit,
        onNavigateToAds: () -> Unit
    ) {
        if (_uiState.value.isConnecting) return

        if (_uiState.value.isCredentialValid) {
            Log.d("HomeViewModel", "Credential valid. Navigating to Ads screen.")
            onNavigateToAds()
        } else {
            Log.d("HomeViewModel", "Navigating to Pairing selection.")
            onNavigateToPairing()
        }
    }
}
