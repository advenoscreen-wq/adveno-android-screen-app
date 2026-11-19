package com.adveno.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * - If valid: Button displays "Start Ads" and navigates to the Ads screen.
 * - If invalid or missing: Button displays "Start Connecting" and navigates to the pairing selection screen.
 */
class HomeViewModel(
    private val verifyCredentialUseCase: VerifyCredentialUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkAndVerifyCredential()
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
                    isCredentialValid = false
                )
            }
            return
        }

        Log.d("HomeViewModel", "Local credential found. Verifying with backend...")
        _uiState.update {
            it.copy(
                buttonText = "Connecting...",
                isConnecting = true,
                isCredentialValid = false
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
                                    isCredentialValid = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    buttonText = "Start Connecting",
                                    isConnecting = false,
                                    isCredentialValid = false
                                )
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.w("HomeViewModel", "Credential verification failed: ${result.error.message}")
                        _uiState.update {
                            it.copy(
                                buttonText = "Start Connecting",
                                isConnecting = false,
                                isCredentialValid = false
                            )
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
