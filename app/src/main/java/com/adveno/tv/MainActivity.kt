package com.adveno.tv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adveno.tv.core.navigation.Screen
import com.adveno.tv.presentation.screens.connect.ConnectScreen
import com.adveno.tv.presentation.screens.connect.ConnectViewModel
import com.adveno.tv.presentation.screens.connecting.ConnectingScreen
import com.adveno.tv.presentation.screens.connecting.ConnectingViewModel
import com.adveno.tv.presentation.screens.home.HomeScreen
import com.adveno.tv.presentation.screens.home.HomeViewModel
import com.adveno.tv.presentation.screens.scan.ScanToConnectScreen
import com.adveno.tv.presentation.screens.scan.ScanToConnectViewModel
import com.adveno.tv.presentation.screens.syncing.SyncingScreen
import com.adveno.tv.presentation.screens.syncing.SyncingViewModel
import com.adveno.tv.presentation.theme.AdvenoTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val connectViewModel: ConnectViewModel by viewModels()
    private val scanToConnectViewModel: ScanToConnectViewModel by viewModels()
    private val connectingViewModel: ConnectingViewModel by viewModels()
    private val syncingViewModel: SyncingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvenoTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.StartConnecting) }

                when (currentScreen) {
                    Screen.StartConnecting -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToPairing = {
                                currentScreen = Screen.ConnectScreen
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
                                currentScreen = Screen.Syncing
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Screen.Syncing -> {
                        SyncingScreen(
                            viewModel = syncingViewModel,
                            onBackClick = {
                                currentScreen = Screen.Connecting
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
