package com.adveno.tv.data.remote.api

import com.adveno.tv.data.remote.dto.request.ScreenSyncAcknowledgmentRequest
import com.adveno.tv.data.remote.dto.response.AssignedScreenPlaylistResponse
import com.adveno.tv.data.remote.dto.response.ScreenSyncCheckResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit contract for Adveno TV authenticated screen endpoints (/api/v1/screens).
 *
 * Authentication:
 * All endpoints require `Authorization: Bearer <screen-credential>` injected automatically
 * via [com.adveno.tv.core.network.AuthInterceptor].
 */
interface ScreenApiService {

    /**
     * Heartbeat & synchronization check (POST /api/v1/screens/sync-check).
     * Updates last_seen_at and checks if desired_revision > applied_revision.
     */
    @POST("screens/sync-check")
    suspend fun syncCheck(): Response<ScreenSyncCheckResponse>

    /**
     * Retrieves the screen's assigned playlist (GET /api/v1/screens/assigned-playlist).
     * Note: Returns HTTP 200 with JSON null if no playlist is currently assigned.
     */
    @GET("screens/assigned-playlist")
    suspend fun getAssignedPlaylist(): Response<AssignedScreenPlaylistResponse?>

    /**
     * Confirms successful download and application of a playlist revision
     * (POST /api/v1/screens/sync-acknowledgments).
     * HTTP 202 Accepted with empty response body.
     */
    @POST("screens/sync-acknowledgments")
    suspend fun acknowledgeSync(
        @Body request: ScreenSyncAcknowledgmentRequest
    ): Response<Unit>

    /**
     * Revokes the active screen credential and logs the TV device out
     * (POST /api/v1/screens/logout).
     * HTTP 204 No Content with empty response body.
     */
    @POST("screens/logout")
    suspend fun logout(): Response<Unit>
}

