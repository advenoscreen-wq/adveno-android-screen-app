package com.adveno.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.data.repository.ConnectionRepositoryImpl
import com.adveno.tv.domain.model.ConnectionStatus
import com.adveno.tv.domain.repository.ConnectionRepository
import com.adveno.tv.domain.usecase.ConnectUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for HomeScreen managing presentation state and user intents.
 */
class HomeViewModel(
    private val connectUseCase: ConnectUseCase = ConnectUseCase(ConnectionRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onStartConnecting() {
        if (_uiState.value.isConnecting) return

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
                    buttonText = "Connected"
                )
            }
        }
    }
}

