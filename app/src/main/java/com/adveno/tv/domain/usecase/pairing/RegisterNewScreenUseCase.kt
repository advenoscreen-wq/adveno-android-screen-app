package com.adveno.tv.domain.usecase.pairing

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.screen.Screen
import com.adveno.tv.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow

/**
 * Encapsulates new screen registration on the backend using the active pairing session.
 */
class RegisterNewScreenUseCase(
    private val repository: PairingRepository
) {
    operator fun invoke(
        sessionId: String,
        name: String,
        deviceModel: String? = null,
        appVersion: String? = null
    ): Flow<NetworkResult<Screen>> {
        return repository.registerNewScreen(
            sessionId = sessionId,
            name = name,
            deviceModel = deviceModel,
            appVersion = appVersion
        )
    }
}

