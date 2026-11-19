package com.adveno.tv.domain.usecase.pairing

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.screen.ScreenCredential
import com.adveno.tv.domain.repository.PairingRepository
import kotlinx.coroutines.flow.Flow

class IssueReconnectCredentialUseCase(
    private val repository: PairingRepository
) {
    operator fun invoke(sessionId: String): Flow<NetworkResult<ScreenCredential>> {
        return repository.issueReconnectCredential(sessionId)
    }
}

