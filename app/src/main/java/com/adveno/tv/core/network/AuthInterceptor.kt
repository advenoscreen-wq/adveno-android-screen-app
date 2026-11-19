package com.adveno.tv.core.network

import com.adveno.tv.core.storage.ScreenCredentialStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the device screen authentication header (`Authorization: Bearer <screen-credential>`)
 * to screen-authenticated requests.
 *
 * Rules:
 * 1. Skips requests marked with [NetworkConfig.HEADER_NO_DEVICE_AUTH].
 * 2. Does not overwrite an existing Authorization header.
 * 3. Screen credentials are always passed as a Bearer token in the Authorization header.
 * 4. Clears local credential if server responds with SCREEN_NOT_FOUND, SCREEN_DISCONNECTED, or INVALID_SCREEN_CREDENTIAL.
 */
class AuthInterceptor(
    private val credentialStorage: ScreenCredentialStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Check if device authentication is explicitly disabled for this request
        if (originalRequest.header(NetworkConfig.HEADER_NO_DEVICE_AUTH) != null) {
            val strippedRequest = originalRequest.newBuilder()
                .removeHeader(NetworkConfig.HEADER_NO_DEVICE_AUTH)
                .build()
            return chain.proceed(strippedRequest)
        }

        // If an Authorization header is already present, do not overwrite it
        if (originalRequest.header(NetworkConfig.HEADER_AUTHORIZATION) != null) {
            return chain.proceed(originalRequest)
        }

        val credential = credentialStorage.getCredential()
        val request = if (!credential.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(NetworkConfig.HEADER_AUTHORIZATION, "${NetworkConfig.BEARER_PREFIX}$credential")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        // Automatic credential clearance on permanent revocation
        if (response.code == 401 || response.code == 404 || response.code == 410) {
            // Peek at response body for error code without consuming stream
            val peekedBody = response.peekBody(1024).string()
            if (peekedBody.contains("SCREEN_NOT_FOUND") ||
                peekedBody.contains("SCREEN_DISCONNECTED") ||
                peekedBody.contains("INVALID_SCREEN_CREDENTIAL")
            ) {
                credentialStorage.clearCredential()
            }
        }

        return response
    }
}

