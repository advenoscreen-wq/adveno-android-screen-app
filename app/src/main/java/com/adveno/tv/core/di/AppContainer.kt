package com.adveno.tv.core.di

import android.content.Context
import com.adveno.tv.core.storage.ActivePlaylistStorage
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.remote.api.PairingApiService
import com.adveno.tv.data.remote.api.ScreenApiService
import com.adveno.tv.data.remote.datasource.PairingRemoteDataSource
import com.adveno.tv.data.remote.datasource.ScreenRemoteDataSource
import com.adveno.tv.data.remote.downloader.MediaDownloader
import com.adveno.tv.domain.repository.PairingRepository
import com.adveno.tv.domain.repository.PlaylistRepository
import com.adveno.tv.domain.repository.ScreenRepository
import com.adveno.tv.domain.repository.SyncRepository
import com.adveno.tv.domain.usecase.pairing.CreatePairingSessionUseCase
import com.adveno.tv.domain.usecase.pairing.IssueReconnectCredentialUseCase
import com.adveno.tv.domain.usecase.pairing.PollPairingStatusUseCase
import com.adveno.tv.domain.usecase.pairing.RegisterNewScreenUseCase
import com.adveno.tv.domain.usecase.playlist.GetAssignedPlaylistUseCase
import com.adveno.tv.domain.usecase.screen.LogoutScreenUseCase
import com.adveno.tv.domain.usecase.screen.VerifyCredentialUseCase
import com.adveno.tv.domain.usecase.sync.AcknowledgeSyncUseCase
import com.adveno.tv.domain.usecase.sync.CheckSyncStatusUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Dependency container providing single-source-of-truth component graphs.
 * Follows the official Android Architecture Manual DI pattern.
 */
class AppContainer(val context: Context) {

    // Storage
    val credentialStorage: ScreenCredentialStorage by lazy {
        StorageModule.provideScreenCredentialStorage(context)
    }

    val sessionStorage: PairingSessionStorage by lazy {
        StorageModule.providePairingSessionStorage()
    }

    val activePlaylistStorage: ActivePlaylistStorage by lazy {
        StorageModule.provideActivePlaylistStorage()
    }

    // Network
    val networkMonitor: com.adveno.tv.core.network.NetworkMonitor by lazy {
        com.adveno.tv.core.network.NetworkMonitorImpl(context)
    }

    val okHttpClient: OkHttpClient by lazy {
        NetworkModule.provideOkHttpClient(credentialStorage, sessionStorage)
    }

    val retrofit: Retrofit by lazy {
        NetworkModule.provideRetrofit(okHttpClient)
    }

    val pairingApiService: PairingApiService by lazy {
        NetworkModule.providePairingApiService(retrofit)
    }

    val screenApiService: ScreenApiService by lazy {
        NetworkModule.provideScreenApiService(retrofit)
    }

    // Remote Data Sources
    val pairingRemoteDataSource: PairingRemoteDataSource by lazy {
        DataModule.providePairingRemoteDataSource(pairingApiService)
    }

    val screenRemoteDataSource: ScreenRemoteDataSource by lazy {
        DataModule.provideScreenRemoteDataSource(screenApiService)
    }

    val downloadOkHttpClient: OkHttpClient by lazy {
        NetworkModule.provideDownloadOkHttpClient()
    }

    val mediaDownloader: MediaDownloader by lazy {
        DataModule.provideMediaDownloader(downloadOkHttpClient, context)
    }

    // Repositories
    val pairingRepository: PairingRepository by lazy {
        DataModule.providePairingRepository(pairingRemoteDataSource, sessionStorage, credentialStorage)
    }

    val screenRepository: ScreenRepository by lazy {
        DataModule.provideScreenRepository(screenRemoteDataSource, credentialStorage)
    }

    val playlistRepository: PlaylistRepository by lazy {
        DataModule.providePlaylistRepository(screenRemoteDataSource)
    }

    val syncRepository: SyncRepository by lazy {
        DataModule.provideSyncRepository(screenRemoteDataSource)
    }

    // Use Cases
    val createPairingSessionUseCase: CreatePairingSessionUseCase by lazy {
        UseCaseModule.provideCreatePairingSessionUseCase(pairingRepository)
    }

    val pollPairingStatusUseCase: PollPairingStatusUseCase by lazy {
        UseCaseModule.providePollPairingStatusUseCase(pairingRepository)
    }

    val issueReconnectCredentialUseCase: IssueReconnectCredentialUseCase by lazy {
        UseCaseModule.provideIssueReconnectCredentialUseCase(pairingRepository)
    }

    val registerNewScreenUseCase: RegisterNewScreenUseCase by lazy {
        UseCaseModule.provideRegisterNewScreenUseCase(pairingRepository)
    }

    val logoutScreenUseCase: LogoutScreenUseCase by lazy {
        UseCaseModule.provideLogoutScreenUseCase(screenRepository)
    }

    val verifyCredentialUseCase: VerifyCredentialUseCase by lazy {
        UseCaseModule.provideVerifyCredentialUseCase(screenRepository)
    }

    val getAssignedPlaylistUseCase: GetAssignedPlaylistUseCase by lazy {
        UseCaseModule.provideGetAssignedPlaylistUseCase(playlistRepository)
    }

    val checkSyncStatusUseCase: CheckSyncStatusUseCase by lazy {
        UseCaseModule.provideCheckSyncStatusUseCase(syncRepository)
    }

    val acknowledgeSyncUseCase: AcknowledgeSyncUseCase by lazy {
        UseCaseModule.provideAcknowledgeSyncUseCase(syncRepository)
    }
}

