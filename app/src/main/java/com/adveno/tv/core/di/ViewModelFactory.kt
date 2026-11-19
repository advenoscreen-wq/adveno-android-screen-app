package com.adveno.tv.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.adveno.tv.presentation.screens.ads.AdsRunningViewModel
import com.adveno.tv.presentation.screens.connect.ConnectViewModel
import com.adveno.tv.presentation.screens.connecting.ConnectingViewModel
import com.adveno.tv.presentation.screens.home.HomeViewModel
import com.adveno.tv.presentation.screens.scan.ScanToConnectViewModel
import com.adveno.tv.presentation.screens.syncing.SyncingViewModel

/**
 * Android ViewModelProvider.Factory supplying use case and storage dependencies from [AppContainer].
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    verifyCredentialUseCase = appContainer.verifyCredentialUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ConnectViewModel::class.java) -> {
                ConnectViewModel(
                    createPairingSessionUseCase = appContainer.createPairingSessionUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ScanToConnectViewModel::class.java) -> {
                ScanToConnectViewModel(
                    pollPairingStatusUseCase = appContainer.pollPairingStatusUseCase,
                    createPairingSessionUseCase = appContainer.createPairingSessionUseCase,
                    sessionStorage = appContainer.sessionStorage
                ) as T
            }
            modelClass.isAssignableFrom(ConnectingViewModel::class.java) -> {
                ConnectingViewModel(
                    registerNewScreenUseCase = appContainer.registerNewScreenUseCase,
                    issueReconnectCredentialUseCase = appContainer.issueReconnectCredentialUseCase,
                    sessionStorage = appContainer.sessionStorage
                ) as T
            }
            modelClass.isAssignableFrom(SyncingViewModel::class.java) -> {
                SyncingViewModel() as T
            }
            modelClass.isAssignableFrom(AdsRunningViewModel::class.java) -> {
                AdsRunningViewModel(
                    logoutScreenUseCase = appContainer.logoutScreenUseCase,
                    credentialStorage = appContainer.credentialStorage,
                    sessionStorage = appContainer.sessionStorage,
                    context = appContainer.context
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

