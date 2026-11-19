package com.adveno.tv.presentation.screens.connecting

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.usecase.pairing.IssueReconnectCredentialUseCase
import com.adveno.tv.domain.usecase.pairing.RegisterNewScreenUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ConnectingNavigation {
    data object NavigateToSync : ConnectingNavigation
}

/**
 * ViewModel for the Pairing / Connecting screen.
 * Executes the final pairing API call based on pairing type, persists credentials,
 * triggers the success animation transition, and coordinates navigation to Syncing.
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

    fun startPairingExecution() {
        val sessionId = sessionStorage.getSessionId()
        val pairingType = sessionStorage.getPairingType() ?: PairingType.NEW_SCREEN

        if (sessionId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Missing pairing session. Please restart pairing."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isConnected = false,
                isLoading = true,
                heading = "Connecting...",
                description = "Linking this screen to your Adveno account",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            // Natural transition delay before initiating backend registration
            delay(1500L)

            when (pairingType) {
                PairingType.NEW_SCREEN -> {
                    registerNewScreen(sessionId)
                }
                PairingType.RECONNECT_EXISTING -> {
                    reconnectExistingScreen(sessionId)
                }
            }
        }
    }

    private suspend fun registerNewScreen(sessionId: String) {
        val deviceModel = Build.MODEL.takeIf { !it.isNullOrBlank() } ?: "Android TV"
        val appVersion = "1.0.0"

        registerNewScreenUseCase(
            sessionId = sessionId,
            name = "Adveno Screen TV",
            deviceModel = deviceModel,
            appVersion = appVersion
        ).collect { result ->
            handleResult(result)
        }
    }

    private suspend fun reconnectExistingScreen(sessionId: String) {
        issueReconnectCredentialUseCase(sessionId).collect { result ->
            handleResult(result)
        }
    }

    private suspend fun <T> handleResult(result: NetworkResult<T>) {
        when (result) {
            is NetworkResult.Success -> {
                Log.d("ConnectingViewModel", "Screen pairing successfully finalized! Screen credential saved.")
                _uiState.update {
                    it.copy(
                        isConnected = true,
                        isLoading = false,
                        heading = "Connected!",
                        description = "Your screen is now connected to Adveno.",
                        errorMessage = null
                    )
                }
                // Show Connected success state for 3 seconds before navigating to Syncing Ads
                delay(3000L)
                _navigationEvent.emit(ConnectingNavigation.NavigateToSync)
            }
            is NetworkResult.Error -> {
                val message = result.error.message ?: "Failed to finalize pairing. Please try again."
                Log.e("ConnectingViewModel", "Pairing error: $message")
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

    fun retry() {
        startPairingExecution()
    }

    fun reset() {
        _uiState.value = ConnectingUiState(
            isConnected = false,
            isLoading = true,
            heading = "Connecting...",
            description = "Linking this screen to your Adveno account"
        )
    }
}
