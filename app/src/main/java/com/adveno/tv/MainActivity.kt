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
import com.adveno.tv.presentation.screens.home.HomeScreen
import com.adveno.tv.presentation.screens.home.HomeViewModel
import com.adveno.tv.presentation.screens.scan.ScanToConnectScreen
import com.adveno.tv.presentation.screens.scan.ScanToConnectViewModel
import com.adveno.tv.presentation.theme.AdvenoTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val connectViewModel: ConnectViewModel by viewModels()
    private val scanToConnectViewModel: ScanToConnectViewModel by viewModels()

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
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
