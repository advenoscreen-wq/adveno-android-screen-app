package com.adveno.tv.domain.usecase

import com.adveno.tv.domain.repository.ConnectionRepository

/**
 * Use case encapsulating the business action to start connecting.
 * Keeps ViewModel decoupled from repository details.
 */
class ConnectUseCase(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke() {
        repository.startConnecting()
    }
}

