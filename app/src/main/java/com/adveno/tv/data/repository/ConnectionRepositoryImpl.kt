package com.adveno.tv.data.repository

import com.adveno.tv.domain.model.ConnectionStatus
import com.adveno.tv.domain.repository.ConnectionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete implementation of ConnectionRepository.
 * Manages connection state in-memory until real networking (WebRTC/WebSocket/HTTP) is integrated.
 */
class ConnectionRepositoryImpl : ConnectionRepository {

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val connectionStatus: Flow<ConnectionStatus> = _connectionStatus.asStateFlow()

    override suspend fun startConnecting() {
        _connectionStatus.value = ConnectionStatus.Connecting
        // Simulates connection handshake delay for UI feedback
        delay(1000)
        _connectionStatus.value = ConnectionStatus.Connected
    }

    override suspend fun disconnect() {
        _connectionStatus.value = ConnectionStatus.Disconnected
    }
}

