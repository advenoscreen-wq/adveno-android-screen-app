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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the Adveno screen synchronization workflow:
 * 1. Checks synchronization status (POST /api/v1/screens/sync-check).
 * 2. Retrieves assigned playlist (GET /api/v1/screens/assigned-playlist) if sync is needed.
 * 3. Downloads required image and video media files to app-private storage.
 * 4. Validates integrity and prepares the in-memory active playlist.
 * 5. Acknowledges applied revision (POST /api/v1/screens/sync-acknowledgments).
 * 6. Handles credential revocation (401/404) and transport failures with TV-friendly retries.
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
            Log.w(TAG, "Dependencies not provided. Defaulting to synced demo state.")
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
                    progress = 0.05f,
                    heading = "Syncing Ads...",
                    description = "Checking for updates...",
                    errorMessage = null,
                    canRetry = false
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
                    handleSyncError(syncCheckResult.error, step = "Checking for updates")
                    return@launch
                }
                is NetworkResult.Loading -> {
                    return@launch
                }
            }

            // If sync is not required, keep existing playlist / default dummy and transition directly to Synced
            if (!needSync) {
                Log.d(TAG, "Screen is up-to-date (need_sync = false). Skipping playlist download and acknowledgment.")
                _uiState.update {
                    it.copy(
                        isSynced = true,
                        progress = 1.0f,
                        heading = "Synced!",
                        description = "Your advertisements are ready.",
                        errorMessage = null,
                        canRetry = false
                    )
                }
                return@launch
            }

            // -------------------------------------------------------------
            // STEP 2: Retrieve Assigned Playlist (GET /api/v1/screens/assigned-playlist)
            // -------------------------------------------------------------
            Log.d(TAG, "Step 2: Retrieving assigned playlist...")
            _uiState.update {
                it.copy(
                    progress = 0.15f,
                    heading = "Syncing Ads...",
                    description = "Retrieving playlist..."
                )
            }

            val playlistResult = getAssignedPlaylistUseCase().first()
            if (!isActive) return@launch

            val playlist: Playlist? = when (playlistResult) {
                is NetworkResult.Success -> {
                    playlistResult.data
                }
                is NetworkResult.Error -> {
                    handleSyncError(playlistResult.error, step = "Retrieving playlist")
                    return@launch
                }
                is NetworkResult.Loading -> {
                    return@launch
                }
            }

            // If playlist is null or empty, handle gracefully without crashing or acknowledging an empty revision
            if (playlist == null || playlist.items.isEmpty()) {
                Log.d(TAG, "Assigned playlist is null or empty. Retaining default playback fallback.")
                _uiState.update {
                    it.copy(
                        isSynced = true,
                        progress = 1.0f,
                        heading = "Synced!",
                        description = "Your advertisements are ready.",
                        errorMessage = null,
                        canRetry = false
                    )
                }
                return@launch
            }

            // -------------------------------------------------------------
            // STEP 3: Download Media Files in Position Order
            // -------------------------------------------------------------
            val sortedItems = playlist.items.sortedBy { it.position }
            val totalItems = sortedItems.size
            Log.d(TAG, "Step 3: Downloading $totalItems media assets in ascending order...")

            for ((index, item) in sortedItems.withIndex()) {
                if (!isActive) return@launch

                val media = item.media
                val baseProgress = 0.20f + (index.toFloat() / totalItems) * 0.65f

                _uiState.update {
                    it.copy(
                        progress = baseProgress,
                        heading = "Syncing Ads...",
                        description = "Downloading media ${index + 1} of $totalItems: ${media.name.ifBlank { "Asset" }}"
                    )
                }

                try {
                    val downloadedFile = mediaDownloader.downloadMedia(media) { bytesRead, totalBytes ->
                        if (totalBytes > 0L) {
                            val intraProgress = (bytesRead.toFloat() / totalBytes) * (0.65f / totalItems)
                            val currentTotalProgress = (baseProgress + intraProgress).coerceIn(0.20f, 0.85f)
                            _uiState.update { state -> state.copy(progress = currentTotalProgress) }
                        }
                    }
                    Log.d(TAG, "Media ${media.id} downloaded successfully: ${downloadedFile.name} (${downloadedFile.length()} bytes)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading media ${media.id} (${media.name})", e)
                    val errorDescription = "Failed to download media (${media.name.ifBlank { "asset" }}). Please check connection and try again."
                    _uiState.update {
                        it.copy(
                            heading = "Download Failed",
                            description = errorDescription,
                            errorMessage = errorDescription,
                            canRetry = true
                        )
                    }
                    return@launch
                }
            }

            // -------------------------------------------------------------
            // STEP 4: Validate and Prepare In-Memory Playlist State
            // -------------------------------------------------------------
            Log.d(TAG, "Step 4: Validating downloaded assets and preparing active playlist...")
            _uiState.update {
                it.copy(
                    progress = 0.88f,
                    heading = "Syncing Ads...",
                    description = "Preparing playlist..."
                )
            }

            val preparedItems = sortedItems.map { item ->
                val localFile = mediaDownloader.getMediaFile(item.media)
                if (localFile == null || !localFile.exists() || !localFile.canRead()) {
                    val errorMsg = "Media asset ${item.media.name} is missing or unreadable."
                    Log.e(TAG, errorMsg)
                    _uiState.update {
                        it.copy(
                            heading = "Validation Failed",
                            description = errorMsg,
                            errorMessage = errorMsg,
                            canRetry = true
                        )
                    }
                    return@launch
                }
                item.copy(
                    media = item.media.copy(localFilePath = localFile.absolutePath)
                )
            }

            val preparedPlaylist = playlist.copy(items = preparedItems)
            activePlaylistStorage?.setPlaylist(preparedPlaylist)
            Log.d(TAG, "Prepared playlist successfully stored in-memory: ${preparedPlaylist.name} (version ${preparedPlaylist.version})")

            // -------------------------------------------------------------
            // STEP 5: Acknowledge Synchronization (POST /api/v1/screens/sync-acknowledgments)
            // -------------------------------------------------------------
            val revisionToAcknowledge = desiredRevision ?: playlist.version
            Log.d(TAG, "Step 5: Acknowledging applied revision $revisionToAcknowledge with backend...")
            _uiState.update {
                it.copy(
                    progress = 0.95f,
                    heading = "Syncing Ads...",
                    description = "Confirming synchronization..."
                )
            }

            val ackResult = acknowledgeSyncUseCase(revisionToAcknowledge).first()
            if (!isActive) return@launch

            when (ackResult) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "Revision $revisionToAcknowledge acknowledged successfully.")
                }
                is NetworkResult.Error -> {
                    Log.w(TAG, "Acknowledgment failed: ${ackResult.error.message}")
                    // Downloaded files and prepared playlist remain intact so the user can safely retry acknowledgment
                    val ackErrorMsg = "Failed to confirm synchronization with server. Please try again."
                    _uiState.update {
                        it.copy(
                            heading = "Confirmation Failed",
                            description = ackErrorMsg,
                            errorMessage = ackErrorMsg,
                            canRetry = true
                        )
                    }
                    return@launch
                }
                is NetworkResult.Loading -> {
                    return@launch
                }
            }

            // -------------------------------------------------------------
            // STEP 6: Synchronization Completed Successfully
            // -------------------------------------------------------------
            Log.d(TAG, "Step 6: Synchronization completed successfully.")
            _uiState.update {
                it.copy(
                    isSynced = true,
                    progress = 1.0f,
                    heading = "Synced!",
                    description = "Your advertisements are ready.",
                    errorMessage = null,
                    canRetry = false
                )
            }
        }
    }

    /**
     * Retries synchronization after a network, download, or acknowledgment failure.
     */
    fun retry() {
        syncJob?.cancel()
        syncJob = null
        startSynchronization()
    }

    /**
     * Evaluates errors, distinguishes between credential revocation and transient failures,
     * and sets actionable UI state.
     */
    private fun handleSyncError(error: NetworkError, step: String) {
        when (error) {
            is NetworkError.DeviceRevoked -> {
                Log.w(TAG, "Device credential revoked during $step (${error.errorCode}). Clearing credentials.")
                credentialStorage?.clearCredential()
                activePlaylistStorage?.clearPlaylist()
                _uiState.update {
                    it.copy(
                        isRevoked = true,
                        errorMessage = "Screen credential has been revoked or disconnected.",
                        canRetry = false
                    )
                }
            }
            is NetworkError.NetworkFailure -> {
                Log.w(TAG, "Network failure during $step: ${error.message}")
                val message = "Unable to connect to server. Please check your internet connection."
                _uiState.update {
                    it.copy(
                        heading = "Sync Paused",
                        description = message,
                        errorMessage = message,
                        canRetry = true
                    )
                }
            }
            else -> {
                val message = error.message.takeIf { it.isNotBlank() }
                    ?: "Synchronization failed during $step. Please try again."
                Log.e(TAG, "Error during $step: $message")
                _uiState.update {
                    it.copy(
                        heading = "Sync Paused",
                        description = message,
                        errorMessage = message,
                        canRetry = true
                    )
                }
            }
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

    companion object {
        private const val TAG = "SyncingViewModel"
    }
}
