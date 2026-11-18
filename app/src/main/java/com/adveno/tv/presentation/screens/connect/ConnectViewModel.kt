package com.adveno.tv.presentation.screens.connect

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for Connect Your Screen.
 * Prepares navigation intents for future New Screen and Reconnect pairing workflows.
 */
class ConnectViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    fun onNewScreenSelected(onNavigate: (() -> Unit)? = null) {
        Log.d("ConnectViewModel", "New Screen action triggered from Android TV remote")
        _uiState.update { it.copy(selectedAction = ConnectAction.NewScreen) }
        onNavigate?.invoke()
    }

    fun onReconnectSelected(onNavigate: (() -> Unit)? = null) {
        Log.d("ConnectViewModel", "Reconnect action triggered from Android TV remote")
        _uiState.update { it.copy(selectedAction = ConnectAction.Reconnect) }
        onNavigate?.invoke()
    }
}

