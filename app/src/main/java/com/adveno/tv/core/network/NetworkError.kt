package com.adveno.tv.core.network

/**
 * Domain-facing representation of network and API errors.
 * Completely decouples the Domain layer from OkHttp, Retrofit, or HTTP exceptions.
 */
sealed class NetworkError(open val message: String) {

    /**
     * Standard HTTP errors with backend error details if parsed.
     */
    data class HttpError(
        val httpCode: Int,
        override val message: String,
        val errorCode: String? = null,
        val errorResponse: ApiErrorResponse? = null
    ) : NetworkError(message)

    /**
     * Critical credential revocation errors:
     * - INVALID_SCREEN_CREDENTIAL
     * - SCREEN_NOT_FOUND
     * - SCREEN_DISCONNECTED
     * When encountered, local credentials must be cleared to allow re-pairing.
     */
    data class DeviceRevoked(
        val errorCode: String,
        override val message: String
    ) : NetworkError(message)

    /**
     * Network connectivity failures, socket timeouts, or DNS issues.
     * Sanitizes messages so raw hostnames and technical exceptions are never exposed to the UI.
     */
    data class NetworkFailure(
        val cause: Throwable,
        override val message: String = NetworkExceptionClassifier.USER_FRIENDLY_NETWORK_ERROR
    ) : NetworkError(message)

    /**
     * JSON payload deserialization failure or schema mismatch.
     */
    data class SerializationError(
        override val message: String = "Unable to process server response."
    ) : NetworkError(message)

    /**
     * Fallback for unforeseen errors.
     */
    data class Unknown(
        override val message: String = "An unexpected error occurred",
        val cause: Throwable? = null
    ) : NetworkError(message)

    /**
     * Helper to determine whether this error represents an internet/transport disconnection.
     */
    val isNetworkConnectionIssue: Boolean
        get() = this is NetworkFailure ||
                (this is Unknown && NetworkExceptionClassifier.isNetworkException(cause))
}

