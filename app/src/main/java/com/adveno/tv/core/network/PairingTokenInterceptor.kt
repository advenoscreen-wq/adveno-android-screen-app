package com.adveno.tv.core.network

import com.adveno.tv.core.storage.PairingSessionStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the `X-Pairing-Token` header to pairing-authenticated requests.
 *
 * Rules:
 * 1. Checks for [NetworkConfig.HEADER_REQUIRES_PAIRING_TOKEN].
 * 2. If present, removes the marker and injects `X-Pairing-Token: <token>` from [PairingSessionStorage].
 * 3. Never attaches the pairing token as a Bearer authorization header.
 */
class PairingTokenInterceptor(
    private val sessionStorage: PairingSessionStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Check if pairing token header injection was requested
        val requiresToken = originalRequest.header(NetworkConfig.HEADER_REQUIRES_PAIRING_TOKEN) != null
        val existingHeader = originalRequest.header(NetworkConfig.HEADER_PAIRING_TOKEN)

        val requestBuilder = originalRequest.newBuilder()
            .removeHeader(NetworkConfig.HEADER_REQUIRES_PAIRING_TOKEN)

        // Inject active pairing token if not manually supplied
        if (requiresToken && existingHeader == null) {
            val token = sessionStorage.getPairingToken()
            if (!token.isNullOrBlank()) {
                requestBuilder.header(NetworkConfig.HEADER_PAIRING_TOKEN, token)
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}

