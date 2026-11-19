package com.adveno.tv.domain.repository

import com.adveno.tv.core.network.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for screen device management and session state.
 */
interface ScreenRepository {

    /**
     * Revokes the active screen credential on the backend and clears local storage.
     */
    fun logoutScreen(): Flow<NetworkResult<Unit>>

    /**
     * Returns true if a valid device credential exists in secure local storage.
     */
    fun hasActiveCredential(): Boolean

    /**
     * Clears local credentials directly (e.g. on forced local reset or unrecoverable error).
     */
    fun clearLocalCredentials()
}

