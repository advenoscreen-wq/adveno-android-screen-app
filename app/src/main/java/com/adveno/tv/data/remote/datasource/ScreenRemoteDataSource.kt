package com.adveno.tv.data.remote.datasource

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.network.safeApiCall
import com.adveno.tv.data.remote.api.ScreenApiService
import com.adveno.tv.data.remote.dto.request.ScreenSyncAcknowledgmentRequest
import com.adveno.tv.data.remote.dto.response.AssignedScreenPlaylistResponse
import com.adveno.tv.data.remote.dto.response.ScreenSyncCheckResponse

/**
 * Remote data source abstraction for authenticated screen network operations.
 */
interface ScreenRemoteDataSource {
    suspend fun syncCheck(): NetworkResult<ScreenSyncCheckResponse>

    suspend fun getAssignedPlaylist(): NetworkResult<AssignedScreenPlaylistResponse?>

    suspend fun acknowledgeSync(
        request: ScreenSyncAcknowledgmentRequest
    ): NetworkResult<Unit>

    suspend fun logout(): NetworkResult<Unit>
}

class ScreenRemoteDataSourceImpl(
    private val apiService: ScreenApiService
) : ScreenRemoteDataSource {

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

    override suspend fun logout(): NetworkResult<Unit> {
        return safeApiCall { apiService.logout() }
    }
}

