package com.adveno.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adveno.tv.core.di.ViewModelFactory
import com.adveno.tv.core.navigation.Screen
import com.adveno.tv.presentation.screens.ads.AdsRunningScreen
import com.adveno.tv.presentation.screens.ads.AdsRunningViewModel
import com.adveno.tv.presentation.screens.connect.ConnectScreen
import com.adveno.tv.presentation.screens.connect.ConnectViewModel
import com.adveno.tv.presentation.screens.connecting.ConnectingScreen
import com.adveno.tv.presentation.screens.connecting.ConnectingViewModel
import com.adveno.tv.presentation.screens.home.HomeScreen
import com.adveno.tv.presentation.screens.home.HomeViewModel
import com.adveno.tv.presentation.screens.noads.NoAdsScreen
import com.adveno.tv.presentation.screens.scan.ScanToConnectScreen
import com.adveno.tv.presentation.screens.scan.ScanToConnectViewModel
import com.adveno.tv.presentation.screens.settings.ProtectedSettingsScreen
import com.adveno.tv.presentation.screens.settings.ProtectedSettingsViewModel
import com.adveno.tv.presentation.screens.syncing.SyncingScreen
import com.adveno.tv.presentation.screens.syncing.SyncingViewModel
import com.adveno.tv.presentation.theme.AdvenoTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory: ViewModelFactory by lazy {
        ViewModelFactory((application as AdvenoApplication).container)
    }

    private val homeViewModel: HomeViewModel by viewModels { viewModelFactory }
    private val connectViewModel: ConnectViewModel by viewModels { viewModelFactory }
    private val scanToConnectViewModel: ScanToConnectViewModel by viewModels { viewModelFactory }
    private val connectingViewModel: ConnectingViewModel by viewModels { viewModelFactory }
    private val syncingViewModel: SyncingViewModel by viewModels { viewModelFactory }
    private val adsRunningViewModel: AdsRunningViewModel by viewModels { viewModelFactory }
    private val protectedSettingsViewModel: ProtectedSettingsViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvenoTheme {
                val initialScreen = remember {
                    val container = (application as AdvenoApplication).container
                    if (container.credentialStorage.hasCredential() && container.activePlaylistStorage.hasUsablePlaylist()) {
                        Screen.AdsRunning
                    } else {
                        Screen.StartConnecting
                    }
                }
                var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }

                when (currentScreen) {
                    Screen.StartConnecting -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToPairing = {
                                currentScreen = Screen.ConnectScreen
                            },
                            onNavigateToAds = {
                                currentScreen = if (syncingViewModel.hasLocalAds()) {
                                    Screen.AdsRunning
                                } else {
                                    Screen.NoAds
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.ConnectScreen -> {
                        ConnectScreen(
                            viewModel = connectViewModel,
                            onBackClick = {
                                currentScreen = Screen.StartConnecting
                            },
                            onNavigateToNewScreen = {
                                currentScreen = Screen.ScanToConnect
                            },
                            onNavigateToReconnect = {
                                currentScreen = Screen.ScanToConnect
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.ScanToConnect -> {
                        ScanToConnectScreen(
                            viewModel = scanToConnectViewModel,
                            onBackClick = {
                                currentScreen = Screen.ConnectScreen
                            },
                            onAutoNavigateToConnecting = {
                                currentScreen = Screen.Connecting
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.Connecting -> {
                        ConnectingScreen(
                            viewModel = connectingViewModel,
                            onBackClick = {
                                currentScreen = Screen.ScanToConnect
                            },
                            onNavigateToSync = {
                                syncingViewModel.reset()
                                syncingViewModel.setManualSyncFromAds(false)
                                adsRunningViewModel.reset()
                                currentScreen = Screen.Syncing
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.Syncing -> {
                        SyncingScreen(
                            viewModel = syncingViewModel,
                            onBackClick = {
                                currentScreen = if (syncingViewModel.hasLocalAds()) Screen.AdsRunning else Screen.Connecting
                            },
                            onNavigateToAdsRunning = {
                                currentScreen = Screen.AdsRunning
                            },
                            onNavigateToNoAds = {
                                currentScreen = Screen.NoAds
                            },
                            onReturnToAdsRunning = {
                                currentScreen = Screen.AdsRunning
                            },
                            onNavigateToStartConnecting = {
                                currentScreen = Screen.StartConnecting
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.NoAds -> {
                        val adsRunningState by adsRunningViewModel.uiState.collectAsState()
                        NoAdsScreen(
                            onSyncClick = {
                                syncingViewModel.reset()
                                syncingViewModel.setManualSyncFromAds(false)
                                adsRunningViewModel.reset()
                                currentScreen = Screen.Syncing
                            },
                            onLogoutClick = {
                                adsRunningViewModel.logout(
                                    onSuccess = {
                                        currentScreen = Screen.StartConnecting
                                    }
                                )
                            },
                            isLoggingOut = adsRunningState.isLoggingOut,
                            onReportEventsClick = {
                                adsRunningViewModel.reportEventsManually()
                            },
                            isReportingEvents = adsRunningState.isReportingEvents,
                            reportSuccessMessage = adsRunningState.reportSuccessMessage,
                            reportErrorMessage = adsRunningState.reportErrorMessage,
                            onRetryReport = {
                                adsRunningViewModel.reportEventsManually()
                            },
                            onCancelReport = {
                                adsRunningViewModel.dismissReportError()
                            },
                            onBackClick = {
                                currentScreen = Screen.StartConnecting
                            },
                            onNavigateToProtectedSettings = {
                                currentScreen = Screen.ProtectedSettings
                            },
                            onVerifyPin = { pin, onSuccess ->
                                adsRunningViewModel.verifyAdminPin(pin, onSuccess)
                            },
                            isVerifyingPin = adsRunningViewModel.isVerifyingPin,
                            pinErrorMessage = adsRunningViewModel.pinErrorMessage,
                            onClearPinError = {
                                adsRunningViewModel.clearPinError()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.AdsRunning -> {
                        AdsRunningScreen(
                            viewModel = adsRunningViewModel,
                            onLogoutClick = {
                                currentScreen = Screen.StartConnecting
                            },
                            onSyncClick = {
                                syncingViewModel.reset()
                                syncingViewModel.setManualSyncFromAds(true)
                                currentScreen = Screen.Syncing
                            },
                            onNavigateToNoAds = {
                                currentScreen = Screen.NoAds
                            },
                            onNavigateToProtectedSettings = {
                                currentScreen = Screen.ProtectedSettings
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.ProtectedSettings -> {
                        ProtectedSettingsScreen(
                            viewModel = protectedSettingsViewModel,
                            onExit = {
                                currentScreen = if (syncingViewModel.hasLocalAds()) {
                                    Screen.AdsRunning
                                } else {
                                    Screen.NoAds
                                }
                            },
                            onNavigateToSync = {
                                syncingViewModel.reset()
                                syncingViewModel.setManualSyncFromAds(true)
                                currentScreen = Screen.Syncing
                            },
                            onNavigateToStartConnecting = {
                                currentScreen = Screen.StartConnecting
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
