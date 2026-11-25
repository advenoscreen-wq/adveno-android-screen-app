package com.adveno.tv.core.network

import com.adveno.tv.core.storage.AdminSessionStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the temporary admin-session token (`X-Admin-Session: <token>`)
 * to protected administrative requests.
 *
 * Rules:
 * 1. Checks for [NetworkConfig.HEADER_REQUIRES_ADMIN_SESSION] marker header.
 * 2. If present, removes marker and injects `X-Admin-Session: <token>` from [AdminSessionStorage].
 * 3. Never attaches `X-Admin-Session` to ordinary playback, media loading, heartbeat (sync-check),
 *    or background playback-event reporting.
 * 4. Listens for server response indicating session expiration or revocation (e.g. `EXPIRED_SESSION`,
 *    `ADMIN_SESSION_EXPIRED`, `INVALID_ADMIN_SESSION`) and automatically clears local session token.
 */
class AdminSessionInterceptor(
    private val adminSessionStorage: AdminSessionStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requiresAdminSession = originalRequest.header(NetworkConfig.HEADER_REQUIRES_ADMIN_SESSION) != null
        val existingHeader = originalRequest.header(NetworkConfig.HEADER_ADMIN_SESSION)

        val requestBuilder = originalRequest.newBuilder()
            .removeHeader(NetworkConfig.HEADER_REQUIRES_ADMIN_SESSION)

        // Only inject X-Admin-Session if explicitly requested by protected endpoint
        if (requiresAdminSession && existingHeader == null) {
            val token = adminSessionStorage.getAdminSessionToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header(NetworkConfig.HEADER_ADMIN_SESSION, token)
            }
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Detect backend admin-session revocation or expiration
        if (response.code == 401 || response.code == 403) {
            val peekedBody = response.peekBody(1024).string()
            if (peekedBody.contains("EXPIRED_SESSION") ||
                peekedBody.contains("ADMIN_SESSION_EXPIRED") ||
                peekedBody.contains("INVALID_ADMIN_SESSION") ||
                peekedBody.contains("REVOKED_ADMIN_SESSION")
            ) {
                adminSessionStorage.clearAdminSession()
            }
        }

        return response
    }
}
