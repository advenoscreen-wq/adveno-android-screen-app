package com.adveno.tv.data.remote.api

import com.adveno.tv.core.network.NetworkConfig
import com.adveno.tv.data.remote.dto.request.CreatePairedScreenRequest
import com.adveno.tv.data.remote.dto.request.CreateScreenPairingSessionRequest
import com.adveno.tv.data.remote.dto.response.CreatePairedScreenResponse
import com.adveno.tv.data.remote.dto.response.CreateScreenPairingSessionResponse
import com.adveno.tv.data.remote.dto.response.IssuePairedScreenCredentialResponse
import com.adveno.tv.data.remote.dto.response.ScreenPairingSessionStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit contract for Adveno TV pairing endpoints (/api/v1/pairing).
 *
 * Authentication:
 * - Session creation: None.
 * - Status polling & Reconnect issuance: X-Pairing-Token header.
 * - New screen registration: App-User Supabase JWT + X-Pairing-Token header.
 * None of these endpoints accept device-level screen credential Bearer tokens.
 */
interface PairingApiService {

    /**
     * Creates a new 10-minute pairing session (POST /api/v1/pairing/sessions).
     */
    @Headers("${NetworkConfig.HEADER_NO_DEVICE_AUTH}: true")
    @POST("pairing/sessions")
    suspend fun createPairingSession(
        @Body request: CreateScreenPairingSessionRequest
    ): Response<CreateScreenPairingSessionResponse>

    /**
     * Polls the current status of a pairing session (GET /api/v1/pairing/sessions/{session_id}).
     */
    @Headers(
        "${NetworkConfig.HEADER_NO_DEVICE_AUTH}: true",
        "${NetworkConfig.HEADER_REQUIRES_PAIRING_TOKEN}: true"
    )
    @GET("pairing/sessions/{session_id}")
    suspend fun pollPairingStatus(
        @Path("session_id") sessionId: String,
        @Header(NetworkConfig.HEADER_PAIRING_TOKEN) pairingToken: String? = null
    ): Response<ScreenPairingSessionStatusResponse>

    /**
     * Issues a new raw screen credential during reconnect flow (POST /api/v1/pairing/sessions/{session_id}/credential).
     * Revokes any prior credentials and transitions session to PAIRED.
     */
    @Headers(
        "${NetworkConfig.HEADER_NO_DEVICE_AUTH}: true",
        "${NetworkConfig.HEADER_REQUIRES_PAIRING_TOKEN}: true"
    )
    @POST("pairing/sessions/{session_id}/credential")
    suspend fun issueReconnectCredential(
        @Path("session_id") sessionId: String,
        @Header(NetworkConfig.HEADER_PAIRING_TOKEN) pairingToken: String? = null
    ): Response<IssuePairedScreenCredentialResponse>

    /**
     * Registers a new screen device (POST /api/v1/pairing/sessions/{session_id}/screen).
     *
     * IMPORTANT DEPENDENCY:
     * Requires both the active [pairingToken] and an authenticated [appUserBearerToken]
     * (Supabase app-user access token).
     */
    @Headers(
        "${NetworkConfig.HEADER_NO_DEVICE_AUTH}: true",
        "${NetworkConfig.HEADER_REQUIRES_PAIRING_TOKEN}: true"
    )
    @POST("pairing/sessions/{session_id}/screen")
    suspend fun registerNewScreen(
        @Path("session_id") sessionId: String,
        @Header(NetworkConfig.HEADER_AUTHORIZATION) appUserBearerToken: String,
        @Header(NetworkConfig.HEADER_PAIRING_TOKEN) pairingToken: String? = null,
        @Body request: CreatePairedScreenRequest
    ): Response<CreatePairedScreenResponse>
}

