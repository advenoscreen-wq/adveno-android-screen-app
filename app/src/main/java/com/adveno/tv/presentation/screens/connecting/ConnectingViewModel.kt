package com.adveno.tv.presentation.screens.connecting

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.usecase.pairing.IssueReconnectCredentialUseCase
import com.adveno.tv.domain.usecase.pairing.RegisterNewScreenUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ConnectingNavigation {
    data object NavigateToSync : ConnectingNavigation
}

/**
 * ViewModel for the Pairing / Connecting screen.
 * Executes the final pairing API call based on pairing type, persists credentials,
 * manages the 3-attempt automatic retry loop with attempt indicator,
 * and coordinates navigation to Syncing.
 */
class ConnectingViewModel(
    private val registerNewScreenUseCase: RegisterNewScreenUseCase,
    private val issueReconnectCredentialUseCase: IssueReconnectCredentialUseCase,
    private val sessionStorage: PairingSessionStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectingUiState())
    val uiState: StateFlow<ConnectingUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ConnectingNavigation>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val navigationEvent: SharedFlow<ConnectingNavigation> = _navigationEvent.asSharedFlow()

    private var pairingJob: Job? = null

    companion object {
        private const val TAG = "ConnectingViewModel"
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    fun startPairingExecution() {
        val sessionId = sessionStorage.getSessionId()
        val pairingType = sessionStorage.getPairingType() ?: PairingType.NEW_SCREEN

        if (sessionId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isMaxAttemptsReached = true,
                    errorMessage = "Missing pairing session. Please restart pairing."
                )
            }
            return
        }

        executePairingWithRetry(sessionId, pairingType)
    }

    private fun executePairingWithRetry(sessionId: String, pairingType: PairingType) {
        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            var attempt = 1

            while (attempt <= MAX_ATTEMPTS && isActive) {
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isLoading = true,
                        currentAttempt = attempt,
                        maxAttempts = MAX_ATTEMPTS,
                        isMaxAttemptsReached = false,
                        heading = "Connecting...",
                        description = if (attempt == 1) {
                            "Linking this screen to your Adveno account"
                        } else {
                            "Linking this screen to your Adveno account (Attempt $attempt of $MAX_ATTEMPTS)"
                        },
                        errorMessage = null
                    )
                }

                // Initial transition delay on attempt 1, or retry delay on subsequent attempts
                if (attempt == 1) {
                    delay(1500L)
                } else {
                    delay(RETRY_DELAY_MS)
                }

                if (!isActive) return@launch

                Log.d(TAG, "Executing pairing attempt $attempt of $MAX_ATTEMPTS for session $sessionId ($pairingType)...")

                val result: NetworkResult<*> = when (pairingType) {
                    PairingType.NEW_SCREEN -> {
                        val deviceModel = Build.MODEL.takeIf { !it.isNullOrBlank() } ?: "Android TV"
                        val appVersion = "1.0.0"
                        registerNewScreenUseCase(
                            sessionId = sessionId,
                            name = "Adveno Screen TV",
                            deviceModel = deviceModel,
                            appVersion = appVersion
                        ).first()
                    }
                    PairingType.RECONNECT_EXISTING -> {
                        issueReconnectCredentialUseCase(sessionId).first()
                    }
                }

                if (!isActive) return@launch

                when (result) {
                    is NetworkResult.Success -> {
                        Log.d(TAG, "Screen pairing successfully finalized on attempt $attempt! Screen credential saved.")
                        _uiState.update {
                            it.copy(
                                isConnected = true,
                                isLoading = false,
                                heading = "Connected!",
                                description = "Your screen is now connected to Adveno.",
                                errorMessage = null,
                                isMaxAttemptsReached = false
                            )
                        }
                        // Present Connected success state before navigating to Syncing Ads
                        delay(3000L)
                        _navigationEvent.emit(ConnectingNavigation.NavigateToSync)
                        return@launch
                    }
                    is NetworkResult.Error -> {
                        val error = result.error
                        val isPermanent = error is NetworkError.DeviceRevoked ||
                                (error is NetworkError.HttpError && (error.httpCode == 401 || error.httpCode == 404))

                        if (isPermanent) {
                            Log.e(TAG, "Permanent pairing error encountered: ${error.message}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isMaxAttemptsReached = true,
                                    errorMessage = error.message.ifBlank { "Pairing authorization failed. Please restart pairing." }
                                )
                            }
                            return@launch
                        }

                        Log.w(TAG, "Pairing attempt $attempt of $MAX_ATTEMPTS failed: ${error.message}")
                        if (attempt >= MAX_ATTEMPTS) {
                            Log.e(TAG, "Maximum connection attempts ($MAX_ATTEMPTS) reached.")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isMaxAttemptsReached = true,
                                    errorMessage = "Maximum attempts reached. Please contact Adveno support."
                                )
                            }
                            return@launch
                        } else {
                            attempt++
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
     * Restarts the connection workflow from attempt 1.
     */
    fun retry() {
        startPairingExecution()
    }

    fun reset() {
        pairingJob?.cancel()
        pairingJob = null
        _uiState.value = ConnectingUiState(
            isConnected = false,
            isLoading = true,
            currentAttempt = 1,
            maxAttempts = MAX_ATTEMPTS,
            isMaxAttemptsReached = false,
            heading = "Connecting...",
            description = "Linking this screen to your Adveno account"
        )
    }

    override fun onCleared() {
        super.onCleared()
        pairingJob?.cancel()
        pairingJob = null
    }
}
