package com.adveno.tv.presentation.screens.scan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.qr.QrCodeGenerator
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.domain.model.pairing.PairingSessionStatus
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.usecase.pairing.CreatePairingSessionUseCase
import com.adveno.tv.domain.usecase.pairing.PollPairingStatusUseCase
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

sealed interface ScanToConnectNavigation {
    data object NavigateToConnecting : ScanToConnectNavigation
}

/**
 * ViewModel for Scan to Connect.
 * Generates dynamic QR code locally and coordinates the 5-second polling loop.
 */
class ScanToConnectViewModel(
    private val pollPairingStatusUseCase: PollPairingStatusUseCase,
    private val createPairingSessionUseCase: CreatePairingSessionUseCase,
    private val sessionStorage: PairingSessionStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanToConnectUiState())
    val uiState: StateFlow<ScanToConnectUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ScanToConnectNavigation>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val navigationEvent: SharedFlow<ScanToConnectNavigation> = _navigationEvent.asSharedFlow()

    private var pollingJob: Job? = null

    init {
        initializePairingSession()
    }

    /**
     * Reads active session credentials from storage and generates dynamic QR code matrix.
     */
    fun initializePairingSession() {
        val pairingToken = sessionStorage.getPairingToken()
        val sessionId = sessionStorage.getSessionId()

        if (!pairingToken.isNullOrBlank() && !sessionId.isNullOrBlank()) {
            setupQrCodeAndStartPolling(sessionId, pairingToken)
        } else {
            // Re-create session if storage was empty
            refreshSession()
        }
    }

    private fun setupQrCodeAndStartPolling(sessionId: String, pairingToken: String) {
        try {
            val pairingUrl = QrCodeGenerator.buildPairingUrl(pairingToken)
            Log.d("ScanToConnectVM", "Generated pairing deep-link: $pairingUrl")
            val matrix = QrCodeGenerator.generateMatrix(pairingUrl)

            _uiState.update {
                it.copy(
                    qrMatrix = matrix,
                    isQrReady = true,
                    isExpired = false,
                    errorMessage = null,
                    heading = "Scan to Connect",
                    description = "Scan this QR code from your Adveno dashboard to link this screen."
                )
            }

            startPolling(sessionId)
        } catch (e: Exception) {
            Log.e("ScanToConnectVM", "Error generating QR code", e)
            _uiState.update {
                it.copy(
                    errorMessage = "Failed to render QR code: ${e.message}"
                )
            }
        }
    }

    /**
     * Polls GET /api/v1/pairing/sessions/{session_id} every 5 seconds.
     */
    fun startPolling(sessionId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            Log.d("ScanToConnectVM", "Starting 5-second polling loop for session: $sessionId")
            while (isActive) {
                delay(5000L) // Wait exactly 5 seconds before poll

                when (val result = pollPairingStatusUseCase(sessionId).first()) {
                    is NetworkResult.Success -> {
                        val status = result.data
                        Log.d("ScanToConnectVM", "Poll status received: $status")
                        _uiState.update { it.copy(status = status) }

                        when (status) {
                            PairingSessionStatus.CONNECTING, PairingSessionStatus.PAIRED -> {
                                Log.d("ScanToConnectVM", "Status is $status. Navigating to Connecting screen.")
                                _navigationEvent.emit(ScanToConnectNavigation.NavigateToConnecting)
                                break
                            }
                            PairingSessionStatus.EXPIRED -> {
                                Log.d("ScanToConnectVM", "Status is EXPIRED. Halting polling.")
                                _uiState.update {
                                    it.copy(
                                        isExpired = true,
                                        errorMessage = "Pairing session has expired. Please refresh to generate a new QR code."
                                    )
                                }
                                break
                            }
                            PairingSessionStatus.SCANNED -> {
                                _uiState.update {
                                    it.copy(
                                        heading = "QR Code Scanned",
                                        description = "Device detected. Confirming connection..."
                                    )
                                }
                            }
                            PairingSessionStatus.PENDING -> {
                                // Awaiting scan; continue loop
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.w("ScanToConnectVM", "Transient polling error: ${result.error.message}")
                    }
                    is NetworkResult.Loading -> {
                        // Non-blocking background poll
                    }
                }
            }
            pollingJob = null
        }
    }

    /**
     * Refreshes the pairing session when expired or upon manual retry.
     */
    fun refreshSession() {
        stopPolling()
        val type = sessionStorage.getPairingType() ?: PairingType.NEW_SCREEN
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            createPairingSessionUseCase(type).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        setupQrCodeAndStartPolling(result.data.sessionId, result.data.pairingToken)
                    }
                    is NetworkResult.Error -> {
                        val msg = result.error.message ?: "Unable to renew pairing session."
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = msg)
                        }
                    }
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
