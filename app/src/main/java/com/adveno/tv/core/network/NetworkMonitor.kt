package com.adveno.tv.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Interface for observing active device network connectivity.
 * Decouples the UI and ViewModels from Android system services.
 */
interface NetworkMonitor {
    /**
     * Cold Flow emitting network connectivity changes as validated by Android's ConnectivityManager.
     */
    val isConnectedFlow: Flow<Boolean>

    /**
     * Synchronously returns whether the device currently has validated internet capability.
     */
    fun isCurrentlyConnected(): Boolean
}

class NetworkMonitorImpl(
    context: Context
) : NetworkMonitor {

    private val connectivityManager: ConnectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isCurrentlyConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) return false

            // On production devices with Google services, NET_CAPABILITY_VALIDATED confirms WAN reachability.
            // In Android emulators, local development setups, and private signage networks,
            // Google's generate_204 check often does not validate even when internet is fully operational.
            // Therefore, a network is valid if it has NET_CAPABILITY_INTERNET and is not a captive portal.
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val isNotCaptivePortal = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

            isValidated || isNotCaptivePortal
        } catch (e: Exception) {
            false
        }
    }

    override val isConnectedFlow: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val isNotCaptive = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
                val isConnected = hasInternet && (isValidated || isNotCaptive)
                trySend(isConnected)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        // Send current connectivity state immediately
        trySend(isCurrentlyConnected())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            trySend(isCurrentlyConnected())
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()
}
