package com.adveno.tv.presentation.screens.connecting

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkMonitor
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
 * Executes final pairing API call based on pairing type, persists credentials,
 * manages 3-attempt automatic retry loop, handles network disconnects by pausing
 * without consuming attempts, and coordinates navigation to Syncing.
 */
class ConnectingViewModel(
    private val registerNewScreenUseCase: RegisterNewScreenUseCase,
    private val issueReconnectCredentialUseCase: IssueReconnectCredentialUseCase,
    private val sessionStorage: PairingSessionStorage,
    private val networkMonitor: NetworkMonitor? = null
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

        executePairingWithRetry(sessionId, pairingType, initialAttempt = 1)
    }

    private fun executePairingWithRetry(sessionId: String, pairingType: PairingType, initialAttempt: Int) {
        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            var attempt = initialAttempt
            val opName = if (pairingType == PairingType.NEW_SCREEN) "Screen Registration" else "Screen Reconnection"

            while (attempt <= MAX_ATTEMPTS && isActive) {
                // Check network availability before starting operation
                if (networkMonitor?.isCurrentlyConnected() == false) {
                    Log.w(TAG, "No internet before pairing attempt $attempt. Pausing workflow.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentAttempt = attempt,
                            isNoInternetModalVisible = true,
                            interruptedOperation = opName,
                            noInternetStatusMessage = null,
                            isConnectionRestored = false
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isLoading = true,
                        currentAttempt = attempt,
                        maxAttempts = MAX_ATTEMPTS,
                        isMaxAttemptsReached = false,
                        isNoInternetModalVisible = false,
                        heading = "Connecting...",
                        description = "Linking this screen to your Adveno account",
                        errorMessage = null
                    )
                }

                if (attempt > 1) {
                    delay(RETRY_DELAY_MS)
                } else {
                    delay(1500L)
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
                                isMaxAttemptsReached = false,
                                isNoInternetModalVisible = false
                            )
                        }
                        delay(3000L)
                        _navigationEvent.emit(ConnectingNavigation.NavigateToSync)
                        return@launch
                    }
                    is NetworkResult.Error -> {
                        val error = result.error

                        // Network interruption: pause workflow without consuming attempt
                        val isNetworkLoss = error.isNetworkConnectionIssue ||
                                networkMonitor?.isCurrentlyConnected() == false

                        if (isNetworkLoss) {
                            Log.w(TAG, "Network disconnected during pairing attempt $attempt. Pausing workflow.")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentAttempt = attempt,
                                    isNoInternetModalVisible = true,
                                    interruptedOperation = opName,
                                    noInternetStatusMessage = null,
                                    isConnectionRestored = false
                                )
                            }
                            return@launch
                        }

                        // Permanent error
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

                        // Actual operation failure: consume attempt
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
     * Resumes the connection operation from the exact preserved attempt when internet returns.
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

        val preservedAttempt = _uiState.value.currentAttempt
        _uiState.update {
            it.copy(
                isNoInternetModalVisible = false,
                noInternetStatusMessage = null,
                isConnectionRestored = false
            )
        }

        val sessionId = sessionStorage.getSessionId()
        val pairingType = sessionStorage.getPairingType() ?: PairingType.NEW_SCREEN
        if (!sessionId.isNullOrBlank()) {
            executePairingWithRetry(sessionId, pairingType, initialAttempt = preservedAttempt)
        } else {
            startPairingExecution()
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
