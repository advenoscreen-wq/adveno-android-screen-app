package com.adveno.tv.data.remote.datasource

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.network.safeApiCall
import com.adveno.tv.data.remote.api.PairingApiService
import com.adveno.tv.data.remote.dto.request.CreatePairedScreenRequest
import com.adveno.tv.data.remote.dto.request.CreateScreenPairingSessionRequest
import com.adveno.tv.data.remote.dto.response.CreatePairedScreenResponse
import com.adveno.tv.data.remote.dto.response.CreateScreenPairingSessionResponse
import com.adveno.tv.data.remote.dto.response.IssuePairedScreenCredentialResponse
import com.adveno.tv.data.remote.dto.response.ScreenPairingSessionStatusResponse

/**
 * Remote data source abstraction for pairing network operations.
 */
interface PairingRemoteDataSource {
    suspend fun createPairingSession(
        request: CreateScreenPairingSessionRequest
    ): NetworkResult<CreateScreenPairingSessionResponse>

    suspend fun pollPairingStatus(
        sessionId: String,
        pairingToken: String? = null
    ): NetworkResult<ScreenPairingSessionStatusResponse>

    suspend fun issueReconnectCredential(
        sessionId: String,
        pairingToken: String? = null
    ): NetworkResult<IssuePairedScreenCredentialResponse>

    suspend fun registerNewScreen(
        sessionId: String,
        pairingToken: String? = null,
        request: CreatePairedScreenRequest
    ): NetworkResult<CreatePairedScreenResponse>
}

class PairingRemoteDataSourceImpl(
    private val apiService: PairingApiService
) : PairingRemoteDataSource {

    override suspend fun createPairingSession(
        request: CreateScreenPairingSessionRequest
    ): NetworkResult<CreateScreenPairingSessionResponse> {
        return safeApiCall { apiService.createPairingSession(request) }
    }

    override suspend fun pollPairingStatus(
        sessionId: String,
        pairingToken: String?
    ): NetworkResult<ScreenPairingSessionStatusResponse> {
        return safeApiCall { apiService.pollPairingStatus(sessionId, pairingToken) }
    }

    override suspend fun issueReconnectCredential(
        sessionId: String,
        pairingToken: String?
    ): NetworkResult<IssuePairedScreenCredentialResponse> {
        return safeApiCall { apiService.issueReconnectCredential(sessionId, pairingToken) }
    }

    override suspend fun registerNewScreen(
        sessionId: String,
        pairingToken: String?,
        request: CreatePairedScreenRequest
    ): NetworkResult<CreatePairedScreenResponse> {
        return safeApiCall {
            apiService.registerNewScreen(
                sessionId = sessionId,
                pairingToken = pairingToken,
                request = request
            )
        }
    }
}

