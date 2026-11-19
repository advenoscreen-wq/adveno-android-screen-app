package com.adveno.tv.core.di

import com.adveno.tv.core.network.ApiClient
import com.adveno.tv.core.network.NetworkConfig
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.remote.api.PairingApiService
import com.adveno.tv.data.remote.api.ScreenApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object NetworkModule {

    fun provideOkHttpClient(
        credentialStorage: ScreenCredentialStorage,
        sessionStorage: PairingSessionStorage,
        isDebug: Boolean = true
    ): OkHttpClient {
        return ApiClient.createOkHttpClient(
            credentialStorage = credentialStorage,
            sessionStorage = sessionStorage,
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
}

