package com.adveno.tv.domain.usecase.pairing

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.pairing.PairingSession
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow

class CreatePairingSessionUseCase(
    private val repository: PairingRepository
) {
    operator fun invoke(type: PairingType): Flow<NetworkResult<PairingSession>> {
        return repository.createPairingSession(type)
    }
}

