package com.adveno.tv.core.network

import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory and provider for Retrofit and OkHttp networking clients.
 */
object ApiClient {

    /**
     * Shared Json serialization configuration.
     * Configured for lenient parsing, ignoring unrecognized fields,
     * and coercing unexpected nulls/defaults gracefully.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Builds the shared [OkHttpClient] with appropriate timeouts,
     * security interceptors, and redacted HTTP logging.
     */
    fun createOkHttpClient(
        credentialStorage: ScreenCredentialStorage,
        sessionStorage: PairingSessionStorage,
        isDebug: Boolean = true
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            // Securely redact sensitive credential headers
            redactHeader(NetworkConfig.HEADER_AUTHORIZATION)
            redactHeader(NetworkConfig.HEADER_PAIRING_TOKEN)
        }

        return OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(PairingTokenInterceptor(sessionStorage))
            .addInterceptor(AuthInterceptor(credentialStorage))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Builds the [Retrofit] instance configured with base URL, OkHttpClient,
     * and Kotlinx Serialization converter.
     */
    fun createRetrofit(
        okHttpClient: OkHttpClient,
        baseUrl: String = NetworkConfig.DEFAULT_BASE_URL
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}

