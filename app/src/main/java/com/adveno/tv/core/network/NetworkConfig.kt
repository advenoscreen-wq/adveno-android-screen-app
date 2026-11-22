package com.adveno.tv.core.network

/**
 * Centralized network configuration parameters for the Adveno TV application.
 * Defines base URLs, timeouts, and headers while maintaining separation between
 * development and production settings.
 */
object NetworkConfig {
    /**
     * Default base URL for backend API endpoints.
     * Note: 10.0.2.2 represents localhost for Android TV emulators.
     */
    const val DEFAULT_BASE_URL = "https://8817-2a02-2a40-80fc-a400-1bd-9d23-ad2f-f26b.ngrok-free.app/api/v1/"

    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Standard Backend Headers
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_PAIRING_TOKEN = "X-Pairing-Token"
    const val BEARER_PREFIX = "Bearer "

    // Internal routing flags to avoid attaching wrong credentials
    const val HEADER_NO_DEVICE_AUTH = "X-Adveno-No-Device-Auth"
    const val HEADER_REQUIRES_PAIRING_TOKEN = "X-Adveno-Requires-Pairing-Token"
}

