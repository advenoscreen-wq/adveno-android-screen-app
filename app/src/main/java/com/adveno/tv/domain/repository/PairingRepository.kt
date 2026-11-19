package com.adveno.tv.domain.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.pairing.PairingSession
import com.adveno.tv.domain.model.pairing.PairingSessionStatus
import com.adveno.tv.domain.model.pairing.PairingType
import com.adveno.tv.domain.model.screen.Screen
import com.adveno.tv.domain.model.screen.ScreenCredential
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing screen pairing workflows.
 */
interface PairingRepository {

    /**
     * Creates a new 10-minute pairing session for new screen or reconnect flow.
     */
    fun createPairingSession(
        type: PairingType
    ): Flow<NetworkResult<PairingSession>>

    /**
     * Polls the current status of an ongoing pairing session.
     */
    fun getPairingStatus(
        sessionId: String
    ): Flow<NetworkResult<PairingSessionStatus>>

    /**
     * Issues a reconnect credential for an existing screen session.
     * Revokes prior active credentials and automatically saves the new credential.
     */
    fun issueReconnectCredential(
        sessionId: String
    ): Flow<NetworkResult<ScreenCredential>>

    /**
     * Registers a new screen device requiring App-User Supabase JWT.
     * Automatically saves the issued credential.
     */
    fun registerNewScreen(
        sessionId: String,
        userToken: String,
        name: String,
        deviceModel: String? = null,
        appVersion: String? = null
    ): Flow<NetworkResult<Screen>>
}

