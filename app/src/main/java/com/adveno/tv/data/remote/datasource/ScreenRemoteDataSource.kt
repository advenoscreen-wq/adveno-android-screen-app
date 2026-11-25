package com.adveno.tv.data.remote.datasource

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.network.safeApiCall
import com.adveno.tv.data.remote.api.ScreenApiService
import com.adveno.tv.data.remote.dto.request.PlaybackEventBatchRequest
import com.adveno.tv.data.remote.dto.request.ScreenSyncAcknowledgmentRequest
import com.adveno.tv.data.remote.dto.response.AssignedScreenPlaylistResponse
import com.adveno.tv.data.remote.dto.response.PlaybackEventReportResponse
import com.adveno.tv.data.remote.dto.response.ScreenSyncCheckResponse
import com.adveno.tv.data.remote.dto.response.VerifyScreenCredentialResponse

/**
 * Remote data source abstraction for authenticated screen network operations.
 */
interface ScreenRemoteDataSource {
    suspend fun verifyCredential(credential: String): NetworkResult<VerifyScreenCredentialResponse>

    suspend fun syncCheck(): NetworkResult<ScreenSyncCheckResponse>

    suspend fun getAssignedPlaylist(): NetworkResult<AssignedScreenPlaylistResponse?>

    suspend fun acknowledgeSync(
        request: ScreenSyncAcknowledgmentRequest
    ): NetworkResult<Unit>

    suspend fun reportPlaybackEvents(
        request: PlaybackEventBatchRequest,
        adminSession: String? = null
    ): NetworkResult<PlaybackEventReportResponse>

    suspend fun verifyAdminPin(
        pin: String
    ): NetworkResult<com.adveno.tv.data.remote.dto.response.VerifyAdminPinResponse>

    suspend fun logout(
        adminSession: String? = null
    ): NetworkResult<Unit>
}

class ScreenRemoteDataSourceImpl(
    private val apiService: ScreenApiService
) : ScreenRemoteDataSource {

    override suspend fun verifyCredential(credential: String): NetworkResult<VerifyScreenCredentialResponse> {
        return safeApiCall {
            apiService.verifyCredential(
                authHeader = "Bearer $credential"
            )
        }
    }

    override suspend fun syncCheck(): NetworkResult<ScreenSyncCheckResponse> {
        return safeApiCall { apiService.syncCheck() }
    }

    override suspend fun getAssignedPlaylist(): NetworkResult<AssignedScreenPlaylistResponse?> {
        return safeApiCall { apiService.getAssignedPlaylist() }
    }

    override suspend fun acknowledgeSync(
        request: ScreenSyncAcknowledgmentRequest
    ): NetworkResult<Unit> {
        return safeApiCall { apiService.acknowledgeSync(request) }
    }

    override suspend fun reportPlaybackEvents(
        request: PlaybackEventBatchRequest,
        adminSession: String?
    ): NetworkResult<PlaybackEventReportResponse> {
        return safeApiCall { apiService.reportPlaybackEvents(request, adminSession) }
    }

    override suspend fun verifyAdminPin(
        pin: String
    ): NetworkResult<com.adveno.tv.data.remote.dto.response.VerifyAdminPinResponse> {
        return safeApiCall {
            apiService.verifyAdminPin(
                com.adveno.tv.data.remote.dto.request.VerifyAdminPinRequest(pin)
            )
        }
    }

    override suspend fun logout(adminSession: String?): NetworkResult<Unit> {
        return safeApiCall { apiService.logout(adminSession) }
    }
}

