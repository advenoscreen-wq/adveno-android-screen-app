package com.adveno.tv.core.di

import com.adveno.tv.core.network.ApiClient
import com.adveno.tv.core.network.NetworkConfig
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.remote.api.PairingApiService
import com.adveno.tv.data.remote.api.ScreenApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    fun provideOkHttpClient(
        credentialStorage: ScreenCredentialStorage,
        sessionStorage: PairingSessionStorage,
        adminSessionStorage: com.adveno.tv.core.storage.AdminSessionStorage? = null,
        isDebug: Boolean = true
    ): OkHttpClient {
        return ApiClient.createOkHttpClient(
            credentialStorage = credentialStorage,
            sessionStorage = sessionStorage,
            adminSessionStorage = adminSessionStorage,
            isDebug = isDebug
        )
    }

    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        baseUrl: String = NetworkConfig.DEFAULT_BASE_URL
    ): Retrofit {
        return ApiClient.createRetrofit(
            okHttpClient = okHttpClient,
            baseUrl = baseUrl
        )
    }

    fun providePairingApiService(retrofit: Retrofit): PairingApiService {
        return retrofit.create(PairingApiService::class.java)
    }

    fun provideScreenApiService(retrofit: Retrofit): ScreenApiService {
        return retrofit.create(ScreenApiService::class.java)
    }

    /**
     * Dedicated OkHttpClient optimized for media asset downloads (videos and images).
     * Configured with extended streaming timeouts, automatic redirects, and no body logging
     * to prevent high memory usage and buffer overflows during large media transfers.
     */
    fun provideDownloadOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(180L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}

