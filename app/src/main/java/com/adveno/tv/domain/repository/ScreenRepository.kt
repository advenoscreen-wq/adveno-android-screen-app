package com.adveno.tv.domain.repository

import com.adveno.tv.core.network.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for screen device management and session state.
 */
interface ScreenRepository {

    /**
     * Verifies whether the TV's locally stored credential is valid and active on the backend.
     * If the credential is revoked (401) or unknown (404), automatically clears it locally.
     */
    fun verifyCredential(): Flow<NetworkResult<Boolean>>

    /**
     * Revokes the active screen credential on the backend and clears local storage.
     */
    fun logoutScreen(adminSession: String? = null): Flow<NetworkResult<Unit>>

    /**
     * Returns true if a valid device credential exists in secure local storage.
     */
    fun hasActiveCredential(): Boolean

    /**
     * Clears local credentials directly (e.g. on forced local reset or unrecoverable error).
     */
    fun clearLocalCredentials()
}

