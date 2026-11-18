package com.adveno.tv.core.navigation

/**
 * Defines navigation destinations within the Adveno Android TV app.
 * Using a sealed interface allows exhaustive when expressions without external routing dependencies.
 */
sealed interface Screen {
    val route: String

    data object StartConnecting : Screen {
        override val route: String = "start_connecting"
    }

    data object ConnectScreen : Screen {
        override val route: String = "connect_screen"
    }

    data object ScanToConnect : Screen {
        override val route: String = "scan_to_connect"
    }

    data object Connecting : Screen {
        override val route: String = "connecting"
    }

    data object Syncing : Screen {
        override val route: String = "syncing"
    }

    data object AdsRunning : Screen {
        override val route: String = "ads_running"
    }

    // Alias for backward compatibility with initial Home route
    val Home: Screen get() = StartConnecting
}
