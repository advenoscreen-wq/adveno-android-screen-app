package com.adveno.tv.core.network

import com.adveno.tv.core.storage.ScreenCredentialStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the device screen credential header (`X-Pairing-Token: <screen-credential>`)
 * to screen-authenticated requests.
 *
 * Rules:
 * 1. Skips requests marked with [NetworkConfig.HEADER_NO_DEVICE_AUTH].
 * 2. Does not overwrite an existing X-Pairing-Token header.
 * 3. Never places screen credentials into Authorization header (backend expects Header(alias="X-Pairing-Token")).
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

        // If an X-Pairing-Token header is already present, do not overwrite it
        if (originalRequest.header(NetworkConfig.HEADER_PAIRING_TOKEN) != null) {
            return chain.proceed(originalRequest)
        }

        val credential = credentialStorage.getCredential()
        val request = if (!credential.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(NetworkConfig.HEADER_PAIRING_TOKEN, credential)
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

