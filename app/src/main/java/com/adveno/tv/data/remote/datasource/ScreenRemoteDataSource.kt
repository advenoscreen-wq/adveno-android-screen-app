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
        request: PlaybackEventBatchRequest
    ): NetworkResult<PlaybackEventReportResponse>

    suspend fun logout(): NetworkResult<Unit>
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
        request: PlaybackEventBatchRequest
    ): NetworkResult<PlaybackEventReportResponse> {
        return safeApiCall { apiService.reportPlaybackEvents(request) }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        return safeApiCall { apiService.logout() }
    }
}

