package com.adveno.tv.presentation.screens.settings

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkConfig
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkMonitor
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.ActivePlaylistStorage
import com.adveno.tv.core.storage.AdminSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.domain.repository.PlaybackEventRepository
import com.adveno.tv.domain.usecase.event.ReportPlaybackEventsUseCase
import com.adveno.tv.domain.usecase.screen.LogoutScreenUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class ProtectedAction {
    SYNC_ADS,
    REPORT_EVENTS,
    LOGOUT
}

data class ProtectedSettingsUiState(
    val isSyncing: Boolean = false,
    val syncSuccessMessage: String? = null,
    val syncErrorMessage: String? = null,

    val isReportingEvents: Boolean = false,
    val reportSuccessMessage: String? = null,
    val reportErrorMessage: String? = null,

    val isLoggingOut: Boolean = false,
    val logoutErrorMessage: String? = null,

    // Info details
    val isOnline: Boolean = true,
    val baseUrl: String = "",
    val activeRevision: Int = 1,
    val playlistName: String = "None",
    val playlistItemCount: Int = 0,
    val pendingEventsCount: Int = 0,
    val screenId: String = "Unknown",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = "1.0.0",

    // Session & Network dialogs
    val isSessionExpired: Boolean = false,
    val isNoInternetModalVisible: Boolean = false,
    val pendingAction: ProtectedAction? = null
)

class ProtectedSettingsViewModel(
    private val logoutScreenUseCase: LogoutScreenUseCase,
    private val reportPlaybackEventsUseCase: ReportPlaybackEventsUseCase,
    private val activePlaylistStorage: ActivePlaylistStorage,
    private val credentialStorage: ScreenCredentialStorage,
    private val adminSessionStorage: AdminSessionStorage,
    private val playbackEventRepository: PlaybackEventRepository,
    private val networkMonitor: NetworkMonitor,
    private val context: Context
) : ViewModel() {

    private val sessionStartTime = System.currentTimeMillis()
    private var lastUserActivityTime = System.currentTimeMillis()
    private var inactivityTimerJob: Job? = null

    private val _uiState = MutableStateFlow(
        ProtectedSettingsUiState(
            baseUrl = NetworkConfig.DEFAULT_BASE_URL,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            screenId = credentialStorage.getScreenId() ?: "Unknown"
        )
    )
    val uiState: StateFlow<ProtectedSettingsUiState> = _uiState.asStateFlow()

    init {
        refreshInfo()
        startInactivityWatcher()
    }

    /**
     * Updates activity timestamp on user interaction (TV D-pad or click).
     */
    fun onUserInteraction() {
        lastUserActivityTime = System.currentTimeMillis()
    }

    /**
     * Watcher enforcing the 5-minute inactivity and 15-minute absolute session limits.
     */
    private fun startInactivityWatcher() {
        inactivityTimerJob?.cancel()
        inactivityTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000L) // Check every 10 seconds

                val now = System.currentTimeMillis()
                val isInactive = now - lastUserActivityTime >= 5 * 60 * 1000L
                val isMaxLifetimeExceeded = now - sessionStartTime >= 15 * 60 * 1000L
                val isTokenMissing = !adminSessionStorage.hasActiveAdminSession()

                if (isInactive || isMaxLifetimeExceeded || isTokenMissing) {
                    Log.w(TAG, "Admin session expired (inactive=$isInactive, maxLifetime=$isMaxLifetimeExceeded, tokenMissing=$isTokenMissing).")
                    expireSession()
                    break
                }
            }
        }
    }

    /**
     * Refreshes connection, screen, and event counts for the UI info panel.
     */
    fun refreshInfo() {
        viewModelScope.launch {
            val playlist = activePlaylistStorage.getPlaylist()
            val pendingCount = playbackEventRepository.getPendingEventCount()
            val isConnected = networkMonitor.isCurrentlyConnected()

            _uiState.update {
                it.copy(
                    isOnline = isConnected,
                    activeRevision = playlist?.version ?: 1,
                    playlistName = playlist?.name ?: "No active playlist",
                    playlistItemCount = playlist?.items?.size ?: 0,
                    pendingEventsCount = pendingCount,
                    screenId = credentialStorage.getScreenId() ?: "Unknown"
                )
            }
        }
    }

    /**
     * Triggers manual playback event reporting with the active admin session token.
     */
    fun reportPlaybackEvents() {
        onUserInteraction()
        if (_uiState.value.isReportingEvents) return

        if (!networkMonitor.isCurrentlyConnected()) {
            _uiState.update {
                it.copy(
                    isNoInternetModalVisible = true,
                    pendingAction = ProtectedAction.REPORT_EVENTS
                )
            }
            return
        }

        val sessionToken = adminSessionStorage.getAdminSessionToken()
        if (sessionToken.isNullOrBlank()) {
            expireSession()
            return
        }

        _uiState.update {
            it.copy(
                isReportingEvents = true,
                reportErrorMessage = null,
                reportSuccessMessage = null
            )
        }

        viewModelScope.launch {
            val result = reportPlaybackEventsUseCase(adminSession = sessionToken)
            when (result) {
                is NetworkResult.Success -> {
                    val count = result.data
                    val msg = if (count > 0) "Reported $count event(s) successfully." else "No pending events to report."
                    refreshInfo()
                    _uiState.update {
                        it.copy(
                            isReportingEvents = false,
                            reportSuccessMessage = msg
                        )
                    }
                    delay(3500L)
                    _uiState.update { it.copy(reportSuccessMessage = null) }
                }
                is NetworkResult.Error -> {
                    handleApiError(result.error) { errText ->
                        _uiState.update {
                            it.copy(
                                isReportingEvents = false,
                                reportErrorMessage = errText
                            )
                        }
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Executes screen logout/unpair with confirmation.
     */
    fun logout(onSuccess: () -> Unit) {
        onUserInteraction()
        if (_uiState.value.isLoggingOut) return

        if (!networkMonitor.isCurrentlyConnected()) {
            _uiState.update {
                it.copy(
                    isNoInternetModalVisible = true,
                    pendingAction = ProtectedAction.LOGOUT
                )
            }
            return
        }

        val sessionToken = adminSessionStorage.getAdminSessionToken()
        _uiState.update {
            it.copy(
                isLoggingOut = true,
                logoutErrorMessage = null
            )
        }

        viewModelScope.launch {
            logoutScreenUseCase(adminSession = sessionToken).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        Log.d(TAG, "Screen unpair succeeded on backend. Wiping local data.")
                        clearAllLocalData()
                        _uiState.update { it.copy(isLoggingOut = false) }
                        onSuccess()
                    }
                    is NetworkResult.Error -> {
                        val err = result.error
                        if (err is NetworkError.DeviceRevoked) {
                            Log.d(TAG, "Device already revoked. Wiping local data.")
                            clearAllLocalData()
                            _uiState.update { it.copy(isLoggingOut = false) }
                            onSuccess()
                            return@collect
                        }
                        handleApiError(err) { errText ->
                            _uiState.update {
                                it.copy(
                                    isLoggingOut = false,
                                    logoutErrorMessage = errText
                                )
                            }
                        }
                    }
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoggingOut = true) }
                    }
                }
            }
        }
    }

    /**
     * Wipes credentials, admin tokens, and cached media on confirmed logout.
     */
    private fun clearAllLocalData() {
        try {
            credentialStorage.clearCredential()
            adminSessionStorage.clearAdminSession()
            activePlaylistStorage.clearPlaylist()
            context.let { ctx ->
                File(ctx.filesDir, "media").deleteRecursively()
                File(ctx.cacheDir, "media").deleteRecursively()
                File(ctx.cacheDir, "ads").deleteRecursively()
            }
            Log.d(TAG, "Local screen data evicted successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Error evicting local data", e)
        }
    }

    /**
     * Centralized handler for network and session errors.
     */
    private fun handleApiError(error: NetworkError, onStandardError: (String) -> Unit) {
        val message = error.message

        // Detect backend admin-session expiration or revocation
        if (message.contains("EXPIRED_SESSION", ignoreCase = true) ||
            message.contains("ADMIN_SESSION_EXPIRED", ignoreCase = true) ||
            message.contains("INVALID_ADMIN_SESSION", ignoreCase = true)
        ) {
            Log.w(TAG, "Backend rejected admin session: $message. Expiring session.")
            expireSession()
            return
        }

        if (error.isNetworkConnectionIssue) {
            _uiState.update { it.copy(isNoInternetModalVisible = true) }
            return
        }

        onStandardError(message.ifBlank { "Operation failed. Please try again." })
    }

    /**
     * Resumes whichever operation was interrupted by loss of network.
     */
    fun retryPendingNetworkAction(onLogoutSuccess: () -> Unit) {
        _uiState.update { it.copy(isNoInternetModalVisible = false) }
        when (_uiState.value.pendingAction) {
            ProtectedAction.REPORT_EVENTS -> reportPlaybackEvents()
            ProtectedAction.LOGOUT -> logout(onLogoutSuccess)
            ProtectedAction.SYNC_ADS, null -> {}
        }
    }

    fun dismissNoInternetModal() {
        _uiState.update { it.copy(isNoInternetModalVisible = false, pendingAction = null) }
    }

    fun dismissReportError() {
        _uiState.update { it.copy(reportErrorMessage = null) }
    }

    /**
     * Immediately clears the admin session token and marks session as expired.
     */
    private fun expireSession() {
        adminSessionStorage.clearAdminSession()
        inactivityTimerJob?.cancel()
        _uiState.update { it.copy(isSessionExpired = true) }
    }

    /**
     * Gracefully exits Protected Settings, clearing the temporary session token.
     */
    fun exitSettings(onExit: () -> Unit) {
        adminSessionStorage.clearAdminSession()
        inactivityTimerJob?.cancel()
        onExit()
    }

    override fun onCleared() {
        super.onCleared()
        inactivityTimerJob?.cancel()
        adminSessionStorage.clearAdminSession()
    }

    companion object {
        private const val TAG = "ProtectedSettingsVM"
    }
}
