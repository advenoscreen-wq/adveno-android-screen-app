package com.adveno.tv.domain.repository

import com.adveno.tv.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for managing Adveno TV connections.
 * Clean Architecture rule: The domain layer defines the interface; the data layer implements it.
 */
interface ConnectionRepository {
    val connectionStatus: Flow<ConnectionStatus>
    suspend fun startConnecting()
    suspend fun disconnect()
}

