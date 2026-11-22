package com.adveno.tv.presentation.screens.syncing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.core.network.NetworkError
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
 * ViewModel orchestrating the Adveno screen synchronization workflow:
 *
 * Stage A: Playlist Fetch (retries only playlist fetch, max 3 attempts; does not re-check sync status).
 * Stage B: Media Download (retries each media item up to 3 attempts; preserves downloaded files; does not re-fetch playlist).
 * Stage C: Apply & Acknowledge (immediately applies playlist locally; retries acknowledgment up to 3 attempts;
 *          CRITICAL: never blocks or rolls back on acknowledgment failure—continues to Ads playback).
 */
class SyncingViewModel(
    private val checkSyncStatusUseCase: CheckSyncStatusUseCase? = null,
    private val getAssignedPlaylistUseCase: GetAssignedPlaylistUseCase? = null,
    private val acknowledgeSyncUseCase: AcknowledgeSyncUseCase? = null,
    private val mediaDownloader: MediaDownloader? = null,
    private val activePlaylistStorage: ActivePlaylistStorage? = null,
    private val credentialStorage: ScreenCredentialStorage? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncingUiState())
    val uiState: StateFlow<SyncingUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    companion object {
        private const val TAG = "SyncingViewModel"
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    /**
     * Initiates the end-to-end synchronization process.
     * Prevents duplicate simultaneous executions.
     */
    fun startSynchronization() {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "Synchronization already running. Ignoring duplicate trigger.")
            return
        }

        // If dependencies are not supplied (e.g. preview mode or tests without mocks), fallback gracefully
        if (checkSyncStatusUseCase == null || getAssignedPlaylistUseCase == null ||
            acknowledgeSyncUseCase == null || mediaDownloader == null
        ) {
            Log.w(TAG, "Dependencies not provided. Defaulting to synced state.")
            _uiState.update {
                it.copy(
                    isSynced = true,
                    progress = 1.0f,
                    heading = "Synced!",
                    description = "Your advertisements are ready."
                )
            }
            return
        }

        syncJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSynced = false,
                    isRevoked = false,
                    isRecoveryMode = false,
                    progress = 0.05f,
                    heading = "Syncing Ads...",
                    description = "Checking for updates...",
                    errorMessage = null,
                    currentAttempt = 1,
                    maxAttempts = MAX_ATTEMPTS,
                    isAutoRetrying = false
                )
            }

            // -------------------------------------------------------------
            // STEP 1: Check Sync Status (POST /api/v1/screens/sync-check)
            // -------------------------------------------------------------
            Log.d(TAG, "Step 1: Checking sync status...")
            val syncCheckResult = checkSyncStatusUseCase().first()
            if (!isActive) return@launch

            val (needSync, desiredRevision) = when (syncCheckResult) {
                is NetworkResult.Success -> {
                    val data = syncCheckResult.data
                    Log.d(TAG, "Sync check response: needSync=${data.needSync}, desiredRevision=${data.desiredRevision}")
                    Pair(data.needSync, data.desiredRevision)
                }
                is NetworkResult.Error -> {
                    val error = syncCheckResult.error
                    if (error is NetworkError.DeviceRevoked) {
                        handleRevocation(error)
                        return@launch
                    }
                    // Offline or server temporary error during syncCheck -> enter recovery mode
                    // so TV can continue displaying cached offline advertisements
                    Log.w(TAG, "Sync check failed: ${error.message}. Showing recovery options.")
                    _uiState.update {
                        it.copy(
                            isRecoveryMode = true,
                            recoveryTitle = "Unable to synchronize your content.",
                            recoveryMessage = "Could not connect to Adveno server. You can contact support or continue showing existing ads.",
                            isAutoRetrying = false
                        )
                    }
                    return@launch
                }
                is NetworkResult.Loading -> {
                    return@launch
                }
            }

            // If sync is not required, screen is already up to date
            if (!needSync) {
                Log.d(TAG, "Screen is up-to-date (need_sync = false). Transitioning directly to Synced.")
                _uiState.update {
                    it.copy(
                        isSynced = true,
                        progress = 1.0f,
                        heading = "Synced!",
                        description = "Your advertisements are ready.",
                        isRecoveryMode = false,
                        errorMessage = null,
                        isAutoRetrying = false
                    )
                }
                return@launch
            }

            // -------------------------------------------------------------
            // STAGE A: Retrieve Assigned Playlist (GET /api/v1/screens/assigned-playlist)
            // Maximum 3 attempts; does NOT repeat syncCheck.
            // -------------------------------------------------------------
            Log.d(TAG, "Stage A: Fetching assigned playlist with retry mechanism...")
            var playlistAttempt = 1
            var fetchedPlaylist: Playlist? = null

            while (playlistAttempt <= MAX_ATTEMPTS && isActive) {
                _uiState.update {
                    it.copy(
                        progress = 0.15f,
                        heading = "Syncing Ads...",
                        description = if (playlistAttempt == 1) {
                            "Retrieving playlist..."
                        } else {
                            "Retrieving playlist (Attempt $playlistAttempt of $MAX_ATTEMPTS)..."
                        },
                        currentAttempt = playlistAttempt,
                        maxAttempts = MAX_ATTEMPTS,
                        isAutoRetrying = playlistAttempt > 1,
                        isRecoveryMode = false
                    )
                }

                if (playlistAttempt > 1) {
                    delay(RETRY_DELAY_MS)
                }

                if (!isActive) return@launch

                val playlistResult = getAssignedPlaylistUseCase().first()
                if (!isActive) return@launch

                when (playlistResult) {
                    is NetworkResult.Success -> {
                        fetchedPlaylist = playlistResult.data
                        Log.d(TAG, "Stage A: Successfully retrieved playlist '${fetchedPlaylist?.name}' on attempt $playlistAttempt.")
                        break
                    }
                    is NetworkResult.Error -> {
                        val error = playlistResult.error
                        if (error is NetworkError.DeviceRevoked) {
                            handleRevocation(error)
                            return@launch
                        }

                        Log.w(TAG, "Stage A: Fetch playlist attempt $playlistAttempt of $MAX_ATTEMPTS failed: ${error.message}")
                        if (playlistAttempt >= MAX_ATTEMPTS) {
                            Log.e(TAG, "Stage A: All $MAX_ATTEMPTS attempts to fetch playlist failed.")
                            _uiState.update {
                                it.copy(
                                    isRecoveryMode = true,
                                    recoveryTitle = "Unable to synchronize your content.",
                                    recoveryMessage = "Failed to retrieve playlist after $MAX_ATTEMPTS attempts. Please contact Adveno support or continue showing existing ads.",
                                    isAutoRetrying = false
                                )
                            }
                            return@launch
                        } else {
                            playlistAttempt++
                        }
                    }
                    is NetworkResult.Loading -> {}
                }
            }

            val validPlaylist = fetchedPlaylist
            if (validPlaylist == null || validPlaylist.items.isEmpty()) {
                Log.d(TAG, "Assigned playlist is empty. Retaining default playback fallback.")
                _uiState.update {
                    it.copy(
                        isSynced = true,
                        progress = 1.0f,
                        heading = "Synced!",
                        description = "Your advertisements are ready.",
                        isRecoveryMode = false,
                        errorMessage = null,
                        isAutoRetrying = false
                    )
                }
                return@launch
            }

            // -------------------------------------------------------------
            // STAGE B: Download Media Assets with Independent Retries
            // Retry only failed downloads (max 3 attempts each); preserves completed files.
            // Does NOT re-fetch playlist.
            // -------------------------------------------------------------
            val sortedItems = validPlaylist.items.sortedBy { it.position }
            val totalItems = sortedItems.size
            Log.d(TAG, "Stage B: Downloading $totalItems media assets with independent retries...")

            for ((index, item) in sortedItems.withIndex()) {
                if (!isActive) return@launch

                val media = item.media
                val baseProgress = 0.20f + (index.toFloat() / totalItems) * 0.65f

                // Check if already downloaded and valid to avoid redundant network traffic
                val existingFile = mediaDownloader.getMediaFile(media)
                if (existingFile != null && existingFile.exists() && existingFile.length() > 0L) {
                    Log.d(TAG, "Stage B: Media ${media.id} (${media.name}) already cached on disk. Skipping download.")
                    val completedItemProgress = 0.20f + ((index + 1).toFloat() / totalItems) * 0.65f
                    _uiState.update { state -> state.copy(progress = completedItemProgress) }
                    continue
                }

                var downloadAttempt = 1
                var downloadSuccess = false

                while (downloadAttempt <= MAX_ATTEMPTS && isActive) {
                    _uiState.update {
                        it.copy(
                            progress = baseProgress,
                            heading = "Syncing Ads...",
                            description = if (downloadAttempt == 1) {
                                "Downloading media ${index + 1} of $totalItems: ${media.name.ifBlank { "Asset" }}"
                            } else {
                                "Downloading media ${index + 1} of $totalItems (Attempt $downloadAttempt of $MAX_ATTEMPTS): ${media.name.ifBlank { "Asset" }}"
                            },
                            currentAttempt = downloadAttempt,
                            maxAttempts = MAX_ATTEMPTS,
                            isAutoRetrying = downloadAttempt > 1,
                            isRecoveryMode = false
                        )
                    }

                    if (downloadAttempt > 1) {
                        delay(RETRY_DELAY_MS)
                    }

                    if (!isActive) return@launch

                    try {
                        val downloadedFile = mediaDownloader.downloadMedia(media) { bytesRead, totalBytes ->
                            if (totalBytes > 0L) {
                                val intraProgress = (bytesRead.toFloat() / totalBytes) * (0.65f / totalItems)
                                val currentTotalProgress = (baseProgress + intraProgress).coerceIn(0.20f, 0.85f)
                                _uiState.update { state -> state.copy(progress = currentTotalProgress) }
                            }
                        }
                        Log.d(TAG, "Stage B: Media ${media.id} downloaded successfully on attempt $downloadAttempt (${downloadedFile.length()} bytes).")
                        downloadSuccess = true
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Stage B: Download attempt $downloadAttempt of $MAX_ATTEMPTS failed for media ${media.id} (${media.name}): ${e.message}")
                        if (downloadAttempt >= MAX_ATTEMPTS) {
                            Log.e(TAG, "Stage B: Maximum download attempts reached for media ${media.id}.")
                            _uiState.update {
                                it.copy(
                                    isRecoveryMode = true,
                                    recoveryTitle = "Unable to synchronize your content.",
                                    recoveryMessage = "Failed to download media (${media.name.ifBlank { "Asset" }}). Please contact Adveno support or continue showing existing ads.",
                                    isAutoRetrying = false
                                )
                            }
                            return@launch
                        } else {
                            downloadAttempt++
                        }
                    }
                }

                if (!downloadSuccess) {
                    return@launch
                }
            }

            // -------------------------------------------------------------
            // STAGE C: Apply Playlist and Acknowledge
            // Validate all files, APPLY playlist locally to ActivePlaylistStorage FIRST.
            // Then attempt backend acknowledgment with max 3 retries.
            // CRITICAL: If acknowledgment fails, do NOT block and do NOT rollback!
            // Continue directly to Ads Running playback.
            // -------------------------------------------------------------
            Log.d(TAG, "Stage C: Validating downloaded assets and preparing active playlist...")
            _uiState.update {
                it.copy(
                    progress = 0.88f,
                    heading = "Syncing Ads...",
                    description = "Preparing playlist...",
                    currentAttempt = 1,
                    isAutoRetrying = false,
                    isRecoveryMode = false
                )
            }

            val preparedItems = sortedItems.map { item ->
                val localFile = mediaDownloader.getMediaFile(item.media)
                if (localFile == null || !localFile.exists() || !localFile.canRead()) {
                    val errorMsg = "Media asset ${item.media.name} is missing or unreadable."
                    Log.e(TAG, errorMsg)
                    _uiState.update {
                        it.copy(
                            isRecoveryMode = true,
                            recoveryTitle = "Unable to synchronize your content.",
                            recoveryMessage = "Media file for ${item.media.name} could not be loaded. Please contact Adveno support or continue showing existing ads.",
                            isAutoRetrying = false
                        )
                    }
                    return@launch
                }
                item.copy(media = item.media.copy(localFilePath = localFile.absolutePath))
            }

            // CRITICAL: Apply the newly verified playlist locally FIRST
            val preparedPlaylist = validPlaylist.copy(items = preparedItems)
            activePlaylistStorage?.setPlaylist(preparedPlaylist)
            Log.d(TAG, "Stage C: Prepared playlist successfully applied locally: ${preparedPlaylist.name} (version ${preparedPlaylist.version})")

            // Send acknowledgment with up to 3 automatic retries
            val revisionToAcknowledge = desiredRevision ?: validPlaylist.version
            var ackAttempt = 1
            var ackSucceeded = false

            while (ackAttempt <= MAX_ATTEMPTS && isActive) {
                _uiState.update {
                    it.copy(
                        progress = (0.90f + (ackAttempt * 0.03f)).coerceAtMost(0.99f),
                        heading = "Syncing Ads...",
                        description = if (ackAttempt == 1) {
                            "Confirming synchronization..."
                        } else {
                            "Confirming synchronization (Attempt $ackAttempt of $MAX_ATTEMPTS)..."
                        },
                        currentAttempt = ackAttempt,
                        maxAttempts = MAX_ATTEMPTS,
                        isAutoRetrying = ackAttempt > 1,
                        isRecoveryMode = false
                    )
                }

                if (ackAttempt > 1) {
                    delay(RETRY_DELAY_MS)
                }

                if (!isActive) return@launch

                val ackResult = acknowledgeSyncUseCase(revisionToAcknowledge).first()
                if (!isActive) return@launch

                when (ackResult) {
                    is NetworkResult.Success -> {
                        Log.d(TAG, "Stage C: Revision $revisionToAcknowledge successfully acknowledged on attempt $ackAttempt.")
                        activePlaylistStorage?.setPendingAcknowledgment(null)
                        ackSucceeded = true
                        break
                    }
                    is NetworkResult.Error -> {
                        val error = ackResult.error
                        if (error is NetworkError.DeviceRevoked) {
                            handleRevocation(error)
                            return@launch
                        }

                        Log.w(TAG, "Stage C: Acknowledgment attempt $ackAttempt of $MAX_ATTEMPTS failed: ${error.message}")
                        if (ackAttempt < MAX_ATTEMPTS) {
                            ackAttempt++
                        } else {
                            // CRITICAL RULE: Acknowledgment retries exhausted.
                            // DO NOT show blocking recovery screen and DO NOT rollback applied playlist.
                            // Record pending acknowledgment for background retry and continue to playback!
                            Log.w(TAG, "Stage C: Backend acknowledgment exhausted all $MAX_ATTEMPTS attempts. Storing pending acknowledgment revision $revisionToAcknowledge and continuing to Ads playback.")
                            activePlaylistStorage?.setPendingAcknowledgment(revisionToAcknowledge)
                            break
                        }
                    }
                    is NetworkResult.Loading -> {}
                }
            }

            // -------------------------------------------------------------
            // COMPLETION: Transition directly to Synced state
            // -------------------------------------------------------------
            Log.d(TAG, "Synchronization complete. Content active. Navigating to Ads playback.")
            _uiState.update {
                it.copy(
                    isSynced = true,
                    progress = 1.0f,
                    heading = "Synced!",
                    description = "Your advertisements are ready.",
                    isRecoveryMode = false,
                    errorMessage = null,
                    isAutoRetrying = false
                )
            }
        }
    }

    /**
     * Retries synchronization from beginning when user triggers it from the Recovery Screen.
     */
    fun retry() {
        syncJob?.cancel()
        syncJob = null
        startSynchronization()
    }

    private fun handleRevocation(error: NetworkError.DeviceRevoked) {
        Log.w(TAG, "Device credential revoked (${error.errorCode}). Clearing credentials.")
        credentialStorage?.clearCredential()
        activePlaylistStorage?.clearPlaylist()
        _uiState.update {
            it.copy(
                isRevoked = true,
                errorMessage = "Screen credential has been revoked or disconnected.",
                isRecoveryMode = false,
                isAutoRetrying = false
            )
        }
    }

    fun reset() {
        syncJob?.cancel()
        syncJob = null
        _uiState.value = SyncingUiState()
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        syncJob = null
    }
}
