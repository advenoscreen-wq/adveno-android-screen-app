package com.adveno.tv.domain.usecase.pairing

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.pairing.PairingSessionStatus
import com.adveno.tv.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow

class PollPairingStatusUseCase(
    private val repository: PairingRepository
) {
    operator fun invoke(sessionId: String): Flow<NetworkResult<PairingSessionStatus>> {
        return repository.getPairingStatus(sessionId)
    }
}

