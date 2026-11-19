package com.adveno.tv.domain.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.sync.ScreenSyncCheck
import com.adveno.tv.domain.model.sync.SyncAcknowledgment
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for synchronization checking and revision acknowledgment.
 */
interface SyncRepository {

    /**
     * Sends heartbeat to backend and checks whether a synchronization is required.
     */
    fun checkSyncStatus(): Flow<NetworkResult<ScreenSyncCheck>>

    /**
     * Confirms that a desired playlist revision was successfully downloaded and applied.
     * Note: Must be called only AFTER the TV screen has verified and cached the media assets.
     */
    fun acknowledgeAppliedRevision(
        appliedRevision: Int
    ): Flow<NetworkResult<SyncAcknowledgment>>
}

