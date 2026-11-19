package com.adveno.tv.domain.usecase.sync

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.sync.SyncAcknowledgment
import com.adveno.tv.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow

/**
 * Acknowledges successful local synchronization of a desired revision.
 * IMPORTANT: Call ONLY after media assets have been completely verified/downloaded.
 */
class AcknowledgeSyncUseCase(
    private val repository: SyncRepository
) {
    operator fun invoke(appliedRevision: Int): Flow<NetworkResult<SyncAcknowledgment>> {
        return repository.acknowledgeAppliedRevision(appliedRevision)
    }
}

