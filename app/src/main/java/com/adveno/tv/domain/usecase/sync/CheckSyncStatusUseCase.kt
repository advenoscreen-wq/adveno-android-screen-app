package com.adveno.tv.domain.usecase.sync

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.sync.ScreenSyncCheck
import com.adveno.tv.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow

class CheckSyncStatusUseCase(
    private val repository: SyncRepository
) {
    operator fun invoke(): Flow<NetworkResult<ScreenSyncCheck>> {
        return repository.checkSyncStatus()
    }
}

