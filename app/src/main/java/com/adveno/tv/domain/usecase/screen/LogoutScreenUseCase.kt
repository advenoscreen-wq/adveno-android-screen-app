package com.adveno.tv.domain.usecase.screen

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.repository.ScreenRepository
import kotlinx.coroutines.flow.Flow

class LogoutScreenUseCase(
    private val repository: ScreenRepository
) {
    operator fun invoke(): Flow<NetworkResult<Unit>> {
        return repository.logoutScreen()
    }
}

