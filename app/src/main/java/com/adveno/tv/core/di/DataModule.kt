package com.adveno.tv.core.di

import android.content.Context
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.remote.api.PairingApiService
import com.adveno.tv.data.remote.api.ScreenApiService
import com.adveno.tv.data.remote.datasource.PairingRemoteDataSource
import com.adveno.tv.data.remote.datasource.PairingRemoteDataSourceImpl
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSourceImpl
import com.adveno.tv.data.remote.downloader.MediaDownloader
import com.adveno.tv.data.remote.downloader.MediaDownloaderImpl
import com.adveno.tv.data.repository.PairingRepositoryImpl
import com.adveno.tv.data.repository.PlaylistRepositoryImpl
import com.adveno.tv.data.repository.ScreenRepositoryImpl
import com.adveno.tv.data.repository.SyncRepositoryImpl
import com.adveno.tv.domain.repository.PairingRepository
import com.adveno.tv.domain.repository.PlaylistRepository
import com.adveno.tv.domain.repository.ScreenRepository
import com.adveno.tv.domain.repository.SyncRepository
import okhttp3.OkHttpClient

object DataModule {

    fun providePairingRemoteDataSource(apiService: PairingApiService): PairingRemoteDataSource {
        return PairingRemoteDataSourceImpl(apiService)
    }

    fun provideScreenRemoteDataSource(apiService: ScreenApiService): ScreenRemoteDataSource {
        return ScreenRemoteDataSourceImpl(apiService)
    }

    fun providePairingRepository(
        remoteDataSource: PairingRemoteDataSource,
        sessionStorage: PairingSessionStorage,
        credentialStorage: ScreenCredentialStorage
    ): PairingRepository {
        return PairingRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionStorage = sessionStorage,
            credentialStorage = credentialStorage
        )
    }

    fun provideScreenRepository(
        remoteDataSource: ScreenRemoteDataSource,
        credentialStorage: ScreenCredentialStorage
    ): ScreenRepository {
        return ScreenRepositoryImpl(
            remoteDataSource = remoteDataSource,
            credentialStorage = credentialStorage
        )
    }

    fun providePlaylistRepository(
        remoteDataSource: ScreenRemoteDataSource
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(
            remoteDataSource = remoteDataSource
        )
    }

    fun provideSyncRepository(
        remoteDataSource: ScreenRemoteDataSource
    ): SyncRepository {
        return SyncRepositoryImpl(
            remoteDataSource = remoteDataSource
        )
    }

    fun provideMediaDownloader(
        okHttpClient: OkHttpClient,
        context: Context
    ): MediaDownloader {
        return MediaDownloaderImpl(
            okHttpClient = okHttpClient,
            context = context
        )
    }
}

