package com.adveno.tv.presentation.screens.ads

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.R
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.ActivePlaylistStorage
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.domain.model.playlist.Playlist
import com.adveno.tv.domain.usecase.screen.LogoutScreenUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel responsible for the digital signage advertisement player:
 * - Automatically loads and plays verified local media from ActivePlaylistStorage.
 * - Manages continuous playlist looping in ascending position order.
 * - Handles both Video and Image items with respective durations.
 * - Seamlessly adapts when new playlists are synchronized.
 * - Falls back to default dummy playback if no backend playlist is active.
 * - Auto-hides overlay controls after 3 seconds of user inactivity.
 * - Safe device logout with local media eviction.
 */
class AdsRunningViewModel(
    private val logoutScreenUseCase: LogoutScreenUseCase? = null,
    private val credentialStorage: ScreenCredentialStorage? = null,
    private val sessionStorage: PairingSessionStorage? = null,
    private val context: Context? = null,
    private val activePlaylistStorage: ActivePlaylistStorage? = null,
    customPlaylist: List<AdItem>? = null
) : ViewModel() {

    // Default dummy playlist fallback when no usable backend playlist is available
    val dummyFallbackPlaylist: List<AdItem> = listOf(
        AdItem.Video(
            id = "dummy_ad_video_1",
            title = "Adveno Video Ad (10s)",
            durationMs = 10_000L,
            rawResId = R.raw.sample_ad_video
        ),
        AdItem.Image(
            id = "dummy_ad_image_2",
            title = "Adveno Display Banner (5s)",
            durationMs = 5_000L,
            drawableResId = R.drawable.sample_ad_image
        )
    )

    private val isCustomPlaylistMode = customPlaylist != null

    private var activeMediaList: List<AdItem> = customPlaylist ?: resolveInitialPlaylist()
    val mediaPlaylist: List<AdItem> get() = activeMediaList

    /**
     * Shared in-memory active playlist prepared and applied by the synchronization engine.
     */
    val activePlaylist: StateFlow<Playlist?>? = activePlaylistStorage?.currentPlaylist

    fun getActivePlaylist(): Playlist? = activePlaylistStorage?.getPlaylist()

    private var currentIndex = 0
    private var inactivityJob: Job? = null
    private var adPlaybackJob: Job? = null

    private val _uiState = MutableStateFlow(
        AdsRunningUiState(
            playlist = activeMediaList,
            currentIndex = 0,
            currentItem = activeMediaList.firstOrNull(),
            currentProgress = 0f,
            isControlsVisible = true
        )
    )
    val uiState: StateFlow<AdsRunningUiState> = _uiState.asStateFlow()

    init {
        if (!isCustomPlaylistMode) {
            observeActivePlaylistStorage()
        }
    }

    private fun resolveInitialPlaylist(): List<AdItem> {
        val initialPlaylist = activePlaylistStorage?.getPlaylist()
        val mapped = mapPlaylistToAdItems(initialPlaylist)
        return if (mapped.isNotEmpty()) mapped else dummyFallbackPlaylist
    }

    /**
     * Observes ActivePlaylistStorage for updates (e.g. after a manual or automatic Sync).
     * Automatically applies new playlists without restarting the application.
     */
    private fun observeActivePlaylistStorage() {
        viewModelScope.launch {
            activePlaylistStorage?.currentPlaylist?.collect { playlist ->
                val mapped = mapPlaylistToAdItems(playlist)
                val targetList = if (mapped.isNotEmpty()) mapped else dummyFallbackPlaylist

                if (activeMediaList != targetList) {
                    Log.d(TAG, "New playlist detected (${targetList.size} items). Switching playback.")
                    activeMediaList = targetList
                    currentIndex = 0
                    adPlaybackJob?.cancel()
                    _uiState.update {
                        it.copy(
                            playlist = activeMediaList,
                            currentIndex = 0,
                            currentItem = activeMediaList.firstOrNull(),
                            currentProgress = 0f
                        )
                    }
                    startPlayback()
                }
            }
        }
    }

    /**
     * Maps the backend Playlist domain model to executable local AdItems.
     * Verifies that local media files exist and are readable before inclusion.
     */
    private fun mapPlaylistToAdItems(playlist: Playlist?): List<AdItem> {
        if (playlist == null || playlist.items.isEmpty()) return emptyList()

        val validItems = mutableListOf<AdItem>()
        val sortedItems = playlist.items.sortedBy { it.position }

        for (item in sortedItems) {
            val filePath = item.media.localFilePath
            if (filePath.isNullOrBlank()) {
                Log.w(TAG, "Item ${item.id} skipped: missing localFilePath")
                continue
            }

            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() == 0L) {
                Log.w(TAG, "Item ${item.id} skipped: file missing or empty at $filePath")
                continue
            }

            // Duration: configured durationSeconds on the playlist item preferred
            val durationSec = item.durationSeconds ?: item.media.durationSeconds ?: 10
            val durationMs = (durationSec * 1000L).coerceAtLeast(1000L)

            when (item.media.mediaType) {
                com.adveno.tv.domain.model.media.MediaType.VIDEO -> {
                    validItems.add(
                        AdItem.Video(
                            id = item.id,
                            title = item.media.name.ifBlank { "Video Ad" },
                            durationMs = durationMs,
                            filePath = file.absolutePath
                        )
                    )
                }
                com.adveno.tv.domain.model.media.MediaType.IMAGE -> {
                    val imageDurationSec = item.durationSeconds ?: item.media.durationSeconds ?: 5
                    validItems.add(
                        AdItem.Image(
                            id = item.id,
                            title = item.media.name.ifBlank { "Banner Ad" },
                            durationMs = (imageDurationSec * 1000L).coerceAtLeast(1000L),
                            filePath = file.absolutePath
                        )
                    )
                }
            }
        }

        return validItems
    }

    /**
     * Resets and starts the 3-second auto-hide timer for overlay controls.
     */
    fun startInactivityTimer() {
        _uiState.update { it.copy(isControlsVisible = true) }
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(3000L)
            _uiState.update { it.copy(isControlsVisible = false) }
        }
    }

    /**
     * Triggered on user interaction (TV remote D-pad, OK button, screen touch).
     * Re-reveals the controls and restarts the 3-second inactivity countdown.
     */
    fun onUserInteraction() {
        startInactivityTimer()
    }

    /**
     * Starts or restarts the continuous playback loop from the current item.
     */
    fun startPlayback(startTimer: Boolean = true) {
        if (activeMediaList.isEmpty()) {
            activeMediaList = dummyFallbackPlaylist
        }

        val currentItem = activeMediaList.getOrNull(currentIndex)
            ?: activeMediaList.firstOrNull()
            ?: return

        _uiState.update {
            it.copy(
                currentIndex = currentIndex,
                currentItem = currentItem,
                currentProgress = 0f
            )
        }

        if (startTimer) {
            scheduleCurrentAdTimer(currentItem)
        }
    }

    /**
     * Internal coroutine timer driving the current item duration.
     * Prevents race conditions by capturing expectedIndex.
     */
    private fun scheduleCurrentAdTimer(item: AdItem) {
        adPlaybackJob?.cancel()
        val expectedIndex = currentIndex

        adPlaybackJob = viewModelScope.launch {
            val totalDuration = if (item.durationMs > 0L) item.durationMs else 10_000L
            val stepInterval = 50L
            val totalSteps = (totalDuration / stepInterval).toInt().coerceAtLeast(1)

            for (step in 1..totalSteps) {
                delay(stepInterval)
                if (currentIndex != expectedIndex) return@launch
                val progress = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
                _uiState.update { it.copy(currentProgress = progress) }
            }

            // Duration completed -> loop to the next item
            if (currentIndex == expectedIndex) {
                advanceToNextItem(startTimer = true)
            }
        }
    }

    /**
     * Fine-tunes playback progress if accurate position is provided by the media player.
     */
    fun updateVideoProgress(positionMs: Long, durationMs: Long) {
        if (_uiState.value.currentItem !is AdItem.Video) return
        val safeDuration = if (durationMs > 0L) durationMs else 10_000L
        val progress = (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
        _uiState.update { it.copy(currentProgress = progress) }
    }

    /**
     * Called when the video ad reaches its natural completion.
     */
    fun onVideoCompleted(startTimer: Boolean = true) {
        if (_uiState.value.currentItem is AdItem.Video) {
            advanceToNextItem(startTimer = startTimer)
        }
    }

    /**
     * Called when video playback encounters an error, safely falling back to the next item.
     */
    fun onVideoError(startTimer: Boolean = true) {
        Log.w(TAG, "Video error encountered at index $currentIndex. Advancing.")
        advanceToNextItem(startTimer = startTimer)
    }

    /**
     * Advances to the next item in the playlist and manages the indefinite loop.
     */
    fun advanceToNextItem(startTimer: Boolean = true) {
        if (activeMediaList.isEmpty()) return

        adPlaybackJob?.cancel()
        currentIndex = (currentIndex + 1) % activeMediaList.size
        val nextItem = activeMediaList[currentIndex]

        _uiState.update {
            it.copy(
                currentIndex = currentIndex,
                currentItem = nextItem,
                currentProgress = 0f
            )
        }

        if (startTimer) {
            scheduleCurrentAdTimer(nextItem)
        }
    }

    /**
     * Executes device logout on backend (POST /api/v1/screens/logout) and clears all local data.
     * Shows loading effect on Logout button and displays a graceful TV notification on network errors.
     */
    fun logout(onSuccess: () -> Unit) {
        if (_uiState.value.isLoggingOut) return

        Log.d(TAG, "Logout triggered. Starting logout workflow...")
        _uiState.update {
            it.copy(
                isLoggingOut = true,
                isControlsVisible = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            if (logoutScreenUseCase != null) {
                logoutScreenUseCase.invoke().collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            Log.d(TAG, "Backend logout succeeded. Evicting local credentials and media.")
                            clearAllLocalData()
                            _uiState.update { it.copy(isLoggingOut = false) }
                            reset()
                            onSuccess()
                        }
                        is NetworkResult.Error -> {
                            when (val err = result.error) {
                                is NetworkError.DeviceRevoked -> {
                                    // Device already revoked or disconnected on backend; wipe local state and exit
                                    Log.d(TAG, "Device was already revoked (${err.errorCode}). Wiping local data.")
                                    clearAllLocalData()
                                    _uiState.update { it.copy(isLoggingOut = false) }
                                    reset()
                                    onSuccess()
                                }
                                is NetworkError.NetworkFailure -> {
                                    val errorMsg = "Unable to logout. Please check your internet connection and try again."
                                    Log.w(TAG, errorMsg)
                                    _uiState.update {
                                        it.copy(
                                            isLoggingOut = false,
                                            errorMessage = errorMsg,
                                            isControlsVisible = true
                                        )
                                    }
                                    delay(4000L)
                                    _uiState.update { it.copy(errorMessage = null) }
                                }
                                else -> {
                                    val errorMsg = err.message.takeIf { it.isNotBlank() }
                                        ?: "Logout failed due to a server error. Please try again."
                                    Log.e(TAG, "Logout error: $errorMsg")
                                    _uiState.update {
                                        it.copy(
                                            isLoggingOut = false,
                                            errorMessage = errorMsg,
                                            isControlsVisible = true
                                        )
                                    }
                                    delay(4000L)
                                    _uiState.update { it.copy(errorMessage = null) }
                                }
                            }
                        }
                        is NetworkResult.Loading -> {
                            _uiState.update { it.copy(isLoggingOut = true) }
                        }
                    }
                }
            } else {
                // If use case is null (e.g. preview mode or unconfigured), do NOT clear data silently
                _uiState.update {
                    it.copy(
                        isLoggingOut = false,
                        errorMessage = "Unable to logout. Service unavailable."
                    )
                }
            }
        }
    }

    /**
     * Removes all local storage, screen credentials, pairing session data, and cached media.
     * Only invoked after backend confirms successful logout or credential revocation.
     */
    fun clearAllLocalData() {
        try {
            // 1. Clear secure screen credentials
            credentialStorage?.clearCredential()

            // 2. Clear pairing session storage
            sessionStorage?.clearSession()

            // 3. Clear active in-memory playlist
            activePlaylistStorage?.clearPlaylist()

            // 4. Clear any downloaded media files / ads cache
            context?.let { ctx ->
                val mediaDir = File(ctx.filesDir, "media")
                if (mediaDir.exists()) {
                    mediaDir.deleteRecursively()
                }
                val cacheMediaDir = File(ctx.cacheDir, "media")
                if (cacheMediaDir.exists()) {
                    cacheMediaDir.deleteRecursively()
                }
                val adsCacheDir = File(ctx.cacheDir, "ads")
                if (adsCacheDir.exists()) {
                    adsCacheDir.deleteRecursively()
                }
            }
            Log.d(TAG, "All local credentials, session tokens, and cached media cleared successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing local media or credential storage", e)
        }
    }

    /**
     * Resets the player state, timers, and returns to the first item.
     */
    fun reset() {
        inactivityJob?.cancel()
        adPlaybackJob?.cancel()
        currentIndex = 0
        _uiState.value = AdsRunningUiState(
            playlist = activeMediaList,
            currentIndex = 0,
            currentItem = activeMediaList.firstOrNull(),
            currentProgress = 0f,
            isControlsVisible = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        inactivityJob?.cancel()
        adPlaybackJob?.cancel()
    }

    companion object {
        private const val TAG = "AdsRunningVM"
    }
}
