package com.adveno.tv.domain.usecase.screen

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.repository.ScreenRepository
import kotlinx.coroutines.flow.Flow

/**
 * Checks whether the TV's locally stored credential belongs to an active screen (POST /api/v1/screens/credential/verify).
 */
class VerifyCredentialUseCase(
    private val repository: ScreenRepository
) {
    operator fun invoke(): Flow<NetworkResult<Boolean>> {
        return repository.verifyCredential()
    }

    fun hasActiveCredential(): Boolean {
        return repository.hasActiveCredential()
    }
}

