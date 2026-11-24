package com.adveno.tv.presentation.screens.syncing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkMonitor
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.ActivePlaylistStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.data.remote.downloader.MediaDownloader
import com.adveno.tv.domain.model.playlist.Playlist
import com.adveno.tv.domain.usecase.playlist.GetAssignedPlaylistUseCase
import com.adveno.tv.domain.usecase.sync.AcknowledgeSyncUseCase
import com.adveno.tv.domain.usecase.sync.CheckSyncStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the network-aware resumable Adveno screen synchronization workflow:
 *
 * Stage A: Ads Fetch (retries only Ads fetch, max 3 attempts; does not re-check sync status).
 *          Treats HTTP 200 null as a valid empty state, navigating cleanly to No Ads screen.
 * Stage B: Media Download (retries each media item up to 3 attempts; preserves downloaded files; does not re-fetch Ads).
 * Stage C: Apply & Acknowledge (immediately applies Ads locally; retries acknowledgment up to 3 attempts;
 *          never blocks or rolls back on acknowledgment failure—continues to Ads playback).
 *
 * Manual Sync Recovery:
 * - If manual sync from Ads playback fails after max attempts, returns to Ads playback without interrupting existing Ads.
 * - Initial setup sync failures display the recovery screen with "Sync Now".
 */
class SyncingViewModel(
    private val checkSyncStatusUseCase: CheckSyncStatusUseCase? = null,
    private val getAssignedPlaylistUseCase: GetAssignedPlaylistUseCase? = null,
    private val acknowledgeSyncUseCase: AcknowledgeSyncUseCase? = null,
    private val mediaDownloader: MediaDownloader? = null,
    private val activePlaylistStorage: ActivePlaylistStorage? = null,
    private val credentialStorage: ScreenCredentialStorage? = null,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncingUiState())
    val uiState: StateFlow<SyncingUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    private enum class SyncStage {
        IDLE,
        SYNC_CHECK,
        STAGE_A_FETCH_PLAYLIST,
        STAGE_B_DOWNLOAD_MEDIA,
        STAGE_C_ACKNOWLEDGE
    }

    // Workflow state preservation for seamless resuming
    private var currentStage = SyncStage.IDLE
    private var cachedDesiredRevision: Int? = null
    private var cachedPlaylist: Playlist? = null
    private var cachedMediaIndex: Int = 0
    private var cachedPlaylistAttempt: Int = 1
    private var cachedDownloadAttempt: Int = 1
    private var cachedAckAttempt: Int = 1

    // Tracks if synchronization was initiated manually from Ads playback
    private var isManualSyncFromAds: Boolean = false

    companion object {
        private const val TAG = "SyncingViewModel"
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    init {
        observeNetworkConnectivity()
    }

    private fun observeNetworkConnectivity() {
        if (networkMonitor == null) return
        viewModelScope.launch {
            networkMonitor.isConnectedFlow.collect { isConnected ->
                if (_uiState.value.isNoInternetModalVisible) {
                    _uiState.update {
                        it.copy(
                            isConnectionRestored = isConnected,
                            noInternetStatusMessage = if (isConnected) {
                                "Internet connection restored. Press Retry Connection to continue."
                            } else {
                                "Internet is still unavailable. Please check your network or Wi-Fi settings."
                            }
                        )
                    }
                }
            }
        }
    }

    fun setManualSyncFromAds(isManual: Boolean) {
        this.isManualSyncFromAds = isManual
    }

    fun hasLocalAds(): Boolean {
        return activePlaylistStorage?.hasUsablePlaylist() == true
    }

    /**
     * Initiates or restarts the synchronization process.
     */
    fun startSynchronization() {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "Synchronization already running. Ignoring duplicate trigger.")
            return
        }

        val hasUsableLocal = hasLocalAds()

        if (checkSyncStatusUseCase == null || getAssignedPlaylistUseCase == null ||
            acknowledgeSyncUseCase == null || mediaDownloader == null
        ) {
            Log.w(TAG, "Dependencies not provided. Defaulting to synced state.")
            _uiState.update {
                it.copy(
                    isSynced = true,
                    hasAdsAvailable = hasUsableLocal,
                    hasUsableLocalAds = hasUsableLocal,
                    progress = 1.0f,
                    heading = "Synced!",
                    description = "Your Ads are ready."
                )
            }
            return
        }

        // Reset workflow stages
        currentStage = SyncStage.SYNC_CHECK
        cachedDesiredRevision = null
        cachedPlaylist = null
        cachedMediaIndex = 0
        cachedPlaylistAttempt = 1
        cachedDownloadAttempt = 1
        cachedAckAttempt = 1

        executeWorkflowFromCurrentStage()
    }

    private fun executeWorkflowFromCurrentStage() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            when (currentStage) {
                SyncStage.IDLE, SyncStage.SYNC_CHECK -> {
                    runSyncCheck()
                }
                SyncStage.STAGE_A_FETCH_PLAYLIST -> {
                    runStageAFetchPlaylist()
                }
                SyncStage.STAGE_B_DOWNLOAD_MEDIA -> {
                    runStageBDownloadMedia()
                }
                SyncStage.STAGE_C_ACKNOWLEDGE -> {
                    runStageCApplyAndAcknowledge()
                }
            }
        }
    }

    // -------------------------------------------------------------
    // STEP 1: Check Sync Status (POST /api/v1/screens/sync-check)
    // -------------------------------------------------------------
    private suspend fun runSyncCheck() {
        currentStage = SyncStage.SYNC_CHECK
        val hasLocal = hasLocalAds()

        if (networkMonitor?.isCurrentlyConnected() == false) {
            pauseForNoInternet("Need-Sync Check")
            return
        }

        _uiState.update {
            it.copy(
                isSynced = false,
                isRevoked = false,
                isRecoveryMode = false,
                isManualSyncFailed = false,
                hasUsableLocalAds = hasLocal,
                isNoInternetModalVisible = false,
                progress = 0.05f,
                heading = "Syncing Ads...",
                description = "Checking for updates...",
                errorMessage = null,
                currentAttempt = 1,
                maxAttempts = MAX_ATTEMPTS,
                isAutoRetrying = false
            )
        }

        Log.d(TAG, "Step 1: Checking sync status...")
        val syncCheckResult = checkSyncStatusUseCase!!().first()

        when (syncCheckResult) {
            is NetworkResult.Success -> {
                val data = syncCheckResult.data
                Log.d(TAG, "Sync check response: needSync=${data.needSync}, desiredRevision=${data.desiredRevision}")
                cachedDesiredRevision = data.desiredRevision

                if (!data.needSync) {
                    Log.d(TAG, "Screen is up-to-date (need_sync = false). Transitioning directly to Synced.")
                    currentStage = SyncStage.IDLE
                    _uiState.update {
                        it.copy(
                            isSynced = true,
                            hasAdsAvailable = hasLocal,
                            hasUsableLocalAds = hasLocal,
                            progress = 1.0f,
                            heading = "Synced!",
                            description = "Your Ads are ready.",
                            isRecoveryMode = false,
                            errorMessage = null,
                            isAutoRetrying = false
                        )
                    }
                    return
                }

                // Need sync -> proceed to Stage A
                currentStage = SyncStage.STAGE_A_FETCH_PLAYLIST
                cachedPlaylistAttempt = 1
                runStageAFetchPlaylist()
            }
            is NetworkResult.Error -> {
                val error = syncCheckResult.error
                if (error is NetworkError.DeviceRevoked) {
                    handleRevocation(error)
                    return
                }

                val isNetworkLoss = error.isNetworkConnectionIssue ||
                        networkMonitor?.isCurrentlyConnected() == false

                if (isNetworkLoss) {
                    pauseForNoInternet("Need-Sync Check")
                    return
                }

                if (isManualSyncFromAds) {
                    Log.w(TAG, "Sync check failed during manual sync. Returning to Ads playback.")
                    _uiState.update { it.copy(isManualSyncFailed = true, isRecoveryMode = false) }
                    return
                }

                Log.w(TAG, "Sync check failed: ${error.message}. Showing recovery options.")
                _uiState.update {
                    it.copy(
                        isRecoveryMode = true,
                        hasUsableLocalAds = hasLocal,
                        recoveryTitle = "Unable to synchronize your Ads.",
                        recoveryMessage = "Could not connect to Adveno server. Please contact support or sync to retry.",
                        isAutoRetrying = false
                    )
                }
            }
            is NetworkResult.Loading -> {}
        }
    }

    // -------------------------------------------------------------
    // STAGE A: Retrieve Assigned Ads (GET /api/v1/screens/assigned-playlist)
    // -------------------------------------------------------------
    private suspend fun runStageAFetchPlaylist() {
        currentStage = SyncStage.STAGE_A_FETCH_PLAYLIST
        val hasLocal = hasLocalAds()

        while (cachedPlaylistAttempt <= MAX_ATTEMPTS) {
            if (networkMonitor?.isCurrentlyConnected() == false) {
                pauseForNoInternet("Retrieving Ads")
                return
            }

            _uiState.update {
                it.copy(
                    progress = 0.15f,
                    heading = "Syncing Ads...",
                    description = "Retrieving Ads...",
                    currentAttempt = cachedPlaylistAttempt,
                    maxAttempts = MAX_ATTEMPTS,
                    isAutoRetrying = cachedPlaylistAttempt > 1,
                    isRecoveryMode = false,
                    isNoInternetModalVisible = false
                )
            }

            if (cachedPlaylistAttempt > 1) {
                delay(RETRY_DELAY_MS)
            }

            val playlistResult = getAssignedPlaylistUseCase!!().first()

            when (playlistResult) {
                is NetworkResult.Success -> {
                    cachedPlaylist = playlistResult.data
                    Log.d(TAG, "Stage A: Successfully retrieved Ads response on attempt $cachedPlaylistAttempt: ${cachedPlaylist?.name}")

                    val validPlaylist = cachedPlaylist
                    // If no playlist is assigned (HTTP 200 null) or items is empty:
                    // Treat as valid empty state, not an API failure. Do not retry fetch.
                    // CRITICAL: Even if playlist is null, acknowledge synchronization with the backend!
                    if (validPlaylist == null || validPlaylist.items.isEmpty()) {
                        Log.d(TAG, "No Ads assigned to screen (null or empty items). Clearing active playlist and advancing directly to acknowledgment.")
                        activePlaylistStorage?.clearPlaylist()
                        currentStage = SyncStage.STAGE_C_ACKNOWLEDGE
                        cachedAckAttempt = 1
                        runStageCApplyAndAcknowledge()
                        return
                    }

                    // Playlist valid with items -> advance to Stage B
                    currentStage = SyncStage.STAGE_B_DOWNLOAD_MEDIA
                    cachedMediaIndex = 0
                    cachedDownloadAttempt = 1
                    runStageBDownloadMedia()
                    return
                }
                is NetworkResult.Error -> {
                    val error = playlistResult.error
                    if (error is NetworkError.DeviceRevoked) {
                        handleRevocation(error)
                        return
                    }

                    val isNetworkLoss = error.isNetworkConnectionIssue ||
                            networkMonitor?.isCurrentlyConnected() == false

                    if (isNetworkLoss) {
                        Log.w(TAG, "Network lost during Ads fetch attempt $cachedPlaylistAttempt. Pausing without consuming attempt.")
                        pauseForNoInternet("Retrieving Ads")
                        return
                    }

                    // Operation failure: consume attempt
                    Log.w(TAG, "Stage A: Fetch Ads attempt $cachedPlaylistAttempt of $MAX_ATTEMPTS failed: ${error.message}")
                    if (cachedPlaylistAttempt >= MAX_ATTEMPTS) {
                        Log.e(TAG, "Stage A: All $MAX_ATTEMPTS attempts to fetch Ads failed.")

                        if (isManualSyncFromAds) {
                            Log.w(TAG, "Manual sync from Ads playback failed. Returning to Ads playback.")
                            _uiState.update { it.copy(isManualSyncFailed = true, isRecoveryMode = false) }
                            return
                        }

                        _uiState.update {
                            it.copy(
                                isRecoveryMode = true,
                                hasUsableLocalAds = hasLocal,
                                recoveryTitle = "Unable to synchronize your Ads.",
                                recoveryMessage = "Failed to retrieve Ads after multiple attempts. Please contact Adveno support or sync to retry.",
                                isAutoRetrying = false
                            )
                        }
                        return
                    } else {
                        cachedPlaylistAttempt++
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // -------------------------------------------------------------
    // STAGE B: Download Media Assets with Resumability
    // -------------------------------------------------------------
    private suspend fun runStageBDownloadMedia() {
        currentStage = SyncStage.STAGE_B_DOWNLOAD_MEDIA
        val validPlaylist = cachedPlaylist ?: run {
            currentStage = SyncStage.STAGE_A_FETCH_PLAYLIST
            runStageAFetchPlaylist()
            return
        }

        val sortedItems = validPlaylist.items.sortedBy { it.position }
        val totalItems = sortedItems.size
        val hasLocal = hasLocalAds()
        Log.d(TAG, "Stage B: Downloading $totalItems media assets starting from index $cachedMediaIndex...")

        for (index in cachedMediaIndex until totalItems) {
            cachedMediaIndex = index
            val item = sortedItems[index]
            val media = item.media
            val baseProgress = 0.20f + (index.toFloat() / totalItems) * 0.65f

            // Skip if already cached and valid on local storage
            val existingFile = mediaDownloader!!.getMediaFile(media)
            if (existingFile != null && existingFile.exists() && existingFile.length() > 0L) {
                Log.d(TAG, "Stage B: Media ${media.id} (${media.name}) already cached on disk. Skipping download.")
                val completedItemProgress = 0.20f + ((index + 1).toFloat() / totalItems) * 0.65f
                _uiState.update { state -> state.copy(progress = completedItemProgress) }
                cachedDownloadAttempt = 1
                continue
            }

            var downloadSuccess = false

            while (cachedDownloadAttempt <= MAX_ATTEMPTS) {
                if (networkMonitor?.isCurrentlyConnected() == false) {
                    pauseForNoInternet("Downloading Ad ${index + 1} of $totalItems: ${media.name.ifBlank { "Asset" }}")
                    return
                }

                _uiState.update {
                    it.copy(
                        progress = baseProgress,
                        heading = "Syncing Ads...",
                        description = "Downloading Ad ${index + 1} of $totalItems: ${media.name.ifBlank { "Asset" }}",
                        currentAttempt = cachedDownloadAttempt,
                        maxAttempts = MAX_ATTEMPTS,
                        isAutoRetrying = cachedDownloadAttempt > 1,
                        isRecoveryMode = false,
                        isNoInternetModalVisible = false
                    )
                }

                if (cachedDownloadAttempt > 1) {
                    delay(RETRY_DELAY_MS)
                }

                try {
                    val downloadedFile = mediaDownloader.downloadMedia(media) { bytesRead, totalBytes ->
                        if (totalBytes > 0L) {
                            val intraProgress = (bytesRead.toFloat() / totalBytes) * (0.65f / totalItems)
                            val currentTotalProgress = (baseProgress + intraProgress).coerceIn(0.20f, 0.85f)
                            _uiState.update { state -> state.copy(progress = currentTotalProgress) }
                        }
                    }
                    Log.d(TAG, "Stage B: Media ${media.id} downloaded successfully on attempt $cachedDownloadAttempt (${downloadedFile.length()} bytes).")
                    downloadSuccess = true
                    cachedDownloadAttempt = 1
                    break
                } catch (e: Exception) {
                    val isNetworkLoss = networkMonitor?.isCurrentlyConnected() == false ||
                            com.adveno.tv.core.network.NetworkExceptionClassifier.isNetworkException(e)

                    if (isNetworkLoss) {
                        Log.w(TAG, "Network disconnected while downloading media ${media.name}. Pausing without consuming attempt.")
                        pauseForNoInternet("Downloading Ad ${index + 1} of $totalItems: ${media.name.ifBlank { "Asset" }}")
                        return
                    }

                    Log.w(TAG, "Stage B: Download attempt $cachedDownloadAttempt of $MAX_ATTEMPTS failed for media ${media.id} (${media.name}): ${e.message}")
                    if (cachedDownloadAttempt >= MAX_ATTEMPTS) {
                        Log.e(TAG, "Stage B: Maximum download attempts reached for media ${media.id}.")

                        if (isManualSyncFromAds) {
                            Log.w(TAG, "Manual sync download failed. Returning to Ads playback.")
                            _uiState.update { it.copy(isManualSyncFailed = true, isRecoveryMode = false) }
                            return
                        }

                        _uiState.update {
                            it.copy(
                                isRecoveryMode = true,
                                hasUsableLocalAds = hasLocal,
                                recoveryTitle = "Unable to synchronize your Ads.",
                                recoveryMessage = "Failed to download Ads after multiple attempts. Please contact Adveno support or sync to retry.",
                                isAutoRetrying = false
                            )
                        }
                        return
                    } else {
                        cachedDownloadAttempt++
                    }
                }
            }

            if (!downloadSuccess) {
                return
            }
        }

        // All downloads succeeded -> proceed to Stage C
        currentStage = SyncStage.STAGE_C_ACKNOWLEDGE
        cachedAckAttempt = 1
        runStageCApplyAndAcknowledge()
    }

    // -------------------------------------------------------------
    // STAGE C: Apply Ads and Acknowledge
    // -------------------------------------------------------------
    private suspend fun runStageCApplyAndAcknowledge() {
        currentStage = SyncStage.STAGE_C_ACKNOWLEDGE
        val validPlaylist = cachedPlaylist
        val hasLocal = hasLocalAds()
        val hasAssignedItems = validPlaylist != null && validPlaylist.items.isNotEmpty()

        if (hasAssignedItems) {
            val sortedItems = validPlaylist!!.items.sortedBy { it.position }
            Log.d(TAG, "Stage C: Validating downloaded assets and applying active Ads...")
            _uiState.update {
                it.copy(
                    progress = 0.88f,
                    heading = "Syncing Ads...",
                    description = "Preparing Ads...",
                    currentAttempt = 1,
                    isAutoRetrying = false,
                    isRecoveryMode = false,
                    isNoInternetModalVisible = false
                )
            }

            val preparedItems = sortedItems.map { item ->
                val localFile = mediaDownloader!!.getMediaFile(item.media)
                if (localFile == null || !localFile.exists() || !localFile.canRead()) {
                    val errorMsg = "Media asset ${item.media.name} is missing or unreadable."
                    Log.e(TAG, errorMsg)

                    if (isManualSyncFromAds) {
                        _uiState.update { it.copy(isManualSyncFailed = true, isRecoveryMode = false) }
                        return
                    }

                    _uiState.update {
                        it.copy(
                            isRecoveryMode = true,
                            hasUsableLocalAds = hasLocal,
                            recoveryTitle = "Unable to synchronize your Ads.",
                            recoveryMessage = "Media file for ${item.media.name} could not be loaded. Please contact Adveno support or sync to retry.",
                            isAutoRetrying = false
                        )
                    }
                    return
                }
                item.copy(media = item.media.copy(localFilePath = localFile.absolutePath))
            }

            // CRITICAL RULE: Apply the newly verified Ads locally FIRST
            val preparedPlaylist = validPlaylist.copy(items = preparedItems)
            activePlaylistStorage?.setPlaylist(preparedPlaylist)
            Log.d(TAG, "Stage C: Prepared Ads successfully applied locally: ${preparedPlaylist.name} (version ${preparedPlaylist.version})")
        } else {
            Log.d(TAG, "Stage C: No active Ads assigned. Clearing locally stored playlist data and acknowledging revision.")
            activePlaylistStorage?.clearPlaylist()
            _uiState.update {
                it.copy(
                    progress = 0.88f,
                    heading = "Syncing Ads...",
                    description = "Confirming synchronization...",
                    currentAttempt = 1,
                    isAutoRetrying = false,
                    isRecoveryMode = false,
                    isNoInternetModalVisible = false
                )
            }
        }

        val revisionToAcknowledge = cachedDesiredRevision ?: validPlaylist?.version ?: 1

        while (cachedAckAttempt <= MAX_ATTEMPTS) {
            if (networkMonitor?.isCurrentlyConnected() == false) {
                pauseForNoInternet("Confirming Synchronization")
                return
            }

            _uiState.update {
                it.copy(
                    progress = (0.90f + (cachedAckAttempt * 0.03f)).coerceAtMost(0.99f),
                    heading = "Syncing Ads...",
                    description = "Confirming synchronization...",
                    currentAttempt = cachedAckAttempt,
                    maxAttempts = MAX_ATTEMPTS,
                    isAutoRetrying = cachedAckAttempt > 1,
                    isRecoveryMode = false,
                    isNoInternetModalVisible = false
                )
            }

            if (cachedAckAttempt > 1) {
                delay(RETRY_DELAY_MS)
            }

            val ackResult = acknowledgeSyncUseCase!!(revisionToAcknowledge).first()

            when (ackResult) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "Stage C: Revision $revisionToAcknowledge successfully acknowledged on attempt $cachedAckAttempt.")
                    activePlaylistStorage?.setPendingAcknowledgment(null)
                    break
                }
                is NetworkResult.Error -> {
                    val error = ackResult.error
                    if (error is NetworkError.DeviceRevoked) {
                        handleRevocation(error)
                        return
                    }

                    val isNetworkLoss = error.isNetworkConnectionIssue ||
                            networkMonitor?.isCurrentlyConnected() == false

                    if (isNetworkLoss) {
                        Log.w(TAG, "Network lost during acknowledgment attempt $cachedAckAttempt. Pausing without consuming attempt.")
                        pauseForNoInternet("Confirming Synchronization")
                        return
                    }

                    Log.w(TAG, "Stage C: Acknowledgment attempt $cachedAckAttempt of $MAX_ATTEMPTS failed: ${error.message}")
                    if (cachedAckAttempt < MAX_ATTEMPTS) {
                        cachedAckAttempt++
                    } else {
                        // CRITICAL RULE: Acknowledgment retries exhausted.
                        // DO NOT show blocking recovery screen and DO NOT rollback applied Ads.
                        // Store pending acknowledgment revision for background retry and continue to playback!
                        Log.w(TAG, "Stage C: Backend acknowledgment exhausted all $MAX_ATTEMPTS attempts. Storing pending revision $revisionToAcknowledge and continuing to Ads playback.")
                        activePlaylistStorage?.setPendingAcknowledgment(revisionToAcknowledge)
                        break
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Completion: Transition directly to Synced state
        currentStage = SyncStage.IDLE
        val hasUsableLocal = hasLocalAds()
        val hasAdsAvailable = hasAssignedItems || hasUsableLocal
        Log.d(TAG, "Synchronization complete. Content active. Navigating to Ads playback (hasAdsAvailable=$hasAdsAvailable, hasUsableLocal=$hasUsableLocal).")
        _uiState.update {
            it.copy(
                isSynced = true,
                hasAdsAvailable = hasAdsAvailable,
                hasUsableLocalAds = hasUsableLocal,
                progress = 1.0f,
                heading = "Synced!",
                description = "Your Ads are ready.",
                isRecoveryMode = false,
                errorMessage = null,
                isAutoRetrying = false,
                isNoInternetModalVisible = false
            )
        }
    }

    private fun pauseForNoInternet(operationName: String) {
        Log.w(TAG, "Network interruption detected at '$operationName'. Pausing workflow.")
        _uiState.update {
            it.copy(
                isNoInternetModalVisible = true,
                interruptedOperation = operationName,
                noInternetStatusMessage = null,
                isConnectionRestored = false
            )
        }
    }

    fun retryConnectionFromModal() {
        if (networkMonitor?.isCurrentlyConnected() == false) {
            _uiState.update {
                it.copy(
                    noInternetStatusMessage = "Internet is still unavailable. Please check your network or Wi-Fi settings.",
                    isConnectionRestored = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isNoInternetModalVisible = false,
                noInternetStatusMessage = null,
                isConnectionRestored = false
            )
        }

        Log.d(TAG, "Internet connection confirmed. Resuming workflow from stage: $currentStage")
        executeWorkflowFromCurrentStage()
    }

    fun retry() {
        startSynchronization()
    }

    private fun handleRevocation(error: NetworkError.DeviceRevoked) {
        Log.w(TAG, "Device credential revoked (${error.errorCode}). Clearing credentials.")
        currentStage = SyncStage.IDLE
        credentialStorage?.clearCredential()
        activePlaylistStorage?.clearPlaylist()
        _uiState.update {
            it.copy(
                isRevoked = true,
                errorMessage = "Screen credential has been revoked or disconnected.",
                isRecoveryMode = false,
                isAutoRetrying = false,
                isNoInternetModalVisible = false
            )
        }
    }

    fun reset() {
        syncJob?.cancel()
        syncJob = null
        currentStage = SyncStage.IDLE
        _uiState.value = SyncingUiState()
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        syncJob = null
    }
}
