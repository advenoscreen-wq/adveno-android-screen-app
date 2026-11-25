package com.adveno.tv.presentation.screens.ads

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.R
import com.adveno.tv.core.network.NetworkError
import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.core.storage.ActivePlaylistStorage
import com.adveno.tv.core.storage.PairingSessionStorage
import com.adveno.tv.core.storage.ScreenCredentialStorage
import com.adveno.tv.domain.model.event.PlaybackEvent
import com.adveno.tv.domain.model.playlist.Playlist
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * ViewModel responsible for the digital signage advertisement player:
 * - Automatically loads and plays verified local media from ActivePlaylistStorage.
 * - Manages continuous playlist looping in ascending position order.
 * - Handles both Video and Image items with respective durations.
 * - Seamlessly adapts when new playlists are synchronized.
 * - Falls back to default dummy playback if no backend playlist is active.
 * - Auto-hides overlay controls after 3 seconds of user inactivity.
 * - Safe device logout with local media eviction.
 * - Durably logs ad playback events to SQLite immediately on playback start.
 * - Automatically reports playback events every 30 minutes in batches up to 100.
 * - Excludes the currently playing ad's event from reporting batches.
 * - Supports manual playback event reporting with retry/cancel UI handling.
 */
class AdsRunningViewModel(
    private val logoutScreenUseCase: LogoutScreenUseCase? = null,
    private val credentialStorage: ScreenCredentialStorage? = null,
    private val sessionStorage: PairingSessionStorage? = null,
    private val context: Context? = null,
    private val activePlaylistStorage: ActivePlaylistStorage? = null,
    private val playbackEventRepository: PlaybackEventRepository? = null,
    private val reportPlaybackEventsUseCase: ReportPlaybackEventsUseCase? = null,
    private val verifyAdminPinUseCase: com.adveno.tv.domain.usecase.admin.VerifyAdminPinUseCase? = null,
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
    private var reportingJob: Job? = null

    // Playback event logging & exclusion tracking
    private var currentPlayingEventId: String? = null
    private var pendingNoAdsTransition: Boolean = false

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
        startPeriodicReporting()
    }

    private fun getUtcTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return format.format(Date())
    }

    /**
     * Starts continuous 30-minute background reporting of pending playback events.
     */
    private fun startPeriodicReporting() {
        reportingJob?.cancel()
        reportingJob = viewModelScope.launch {
            while (isActive) {
                delay(30 * 60 * 1000L) // 30 minutes
                try {
                    Log.d(TAG, "Triggering 30-minute automatic playback event reporting...")
                    reportPlaybackEventsUseCase?.invoke(excludeCurrentEventId = currentPlayingEventId)
                } catch (e: Exception) {
                    Log.w(TAG, "Automatic reporting interval exception: ${e.message}")
                }
            }
        }
    }

    /**
     * Immediately triggers manual report of all pending eligible playback events.
     * Excludes the currently playing ad's event.
     */
    fun reportEventsManually() {
        if (_uiState.value.isReportingEvents) return
        _uiState.update {
            it.copy(
                isReportingEvents = true,
                reportErrorMessage = null,
                reportSuccessMessage = null
            )
        }

        viewModelScope.launch {
            val result = reportPlaybackEventsUseCase?.invoke(excludeCurrentEventId = currentPlayingEventId)
            when (result) {
                is NetworkResult.Success -> {
                    val count = result.data
                    val msg = if (count > 0) {
                        "Reported $count event(s) successfully."
                    } else {
                        "No pending playback events to report."
                    }
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
                    val errMessage = result.error.message.takeIf { it.isNotBlank() }
                        ?: "Failed to report playback events. Please check your network connection and retry."
                    _uiState.update {
                        it.copy(
                            isReportingEvents = false,
                            reportErrorMessage = errMessage
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isReportingEvents = false) }
                }
            }
        }
    }

    // Admin PIN verification state
    var isVerifyingPin by mutableStateOf(false)
        private set

    var pinErrorMessage by mutableStateOf<String?>(null)
        private set

    fun verifyAdminPin(pin: String, onSuccess: () -> Unit) {
        if (isVerifyingPin) return
        isVerifyingPin = true
        pinErrorMessage = null

        viewModelScope.launch {
            if (verifyAdminPinUseCase == null) {
                isVerifyingPin = false
                pinErrorMessage = "Admin verification service unavailable."
                return@launch
            }

            val result = verifyAdminPinUseCase(pin)
            when (result) {
                is NetworkResult.Success -> {
                    isVerifyingPin = false
                    pinErrorMessage = null
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    isVerifyingPin = false
                    val err = result.error
                    pinErrorMessage = when {
                        err is NetworkError.DeviceRevoked -> "Screen is not paired or has been disconnected."
                        err.isNetworkConnectionIssue -> "No internet connection. Please check your network."
                        err is NetworkError.HttpError && (err.httpCode == 401 || err.httpCode == 403 || err.httpCode == 400) -> "Invalid PIN. Please try again."
                        else -> err.message.ifBlank { "Invalid PIN. Please try again." }
                    }
                }
                is NetworkResult.Loading -> {
                    isVerifyingPin = true
                }
            }
        }
    }

    fun clearPinError() {
        pinErrorMessage = null
    }

    fun dismissReportError() {
        _uiState.update { it.copy(reportErrorMessage = null) }
    }

    private fun resolveInitialPlaylist(): List<AdItem> {
        val initialPlaylist = activePlaylistStorage?.getPlaylist()
        return mapPlaylistToAdItems(initialPlaylist)
    }

    /**
     * Observes ActivePlaylistStorage for updates (e.g. after a manual or automatic Sync).
     * Automatically applies new Ads without restarting the application or interrupting if invalid.
     */
    private fun observeActivePlaylistStorage() {
        viewModelScope.launch {
            activePlaylistStorage?.currentPlaylist?.collect { playlist ->
                val mapped = mapPlaylistToAdItems(playlist)
                if (mapped.isNotEmpty()) {
                    pendingNoAdsTransition = false
                    val needsPlaybackRestart = activeMediaList != mapped ||
                            _uiState.value.noAdsAvailable ||
                            _uiState.value.currentItem == null ||
                            _uiState.value.playlist.isEmpty()

                    activeMediaList = mapped
                    if (needsPlaybackRestart) {
                        Log.d(TAG, "New or restored Ads detected (${mapped.size} items). Switching playback.")
                        currentIndex = 0
                        adPlaybackJob?.cancel()
                        _uiState.update {
                            it.copy(
                                playlist = activeMediaList,
                                currentIndex = 0,
                                currentItem = activeMediaList.firstOrNull(),
                                currentProgress = 0f,
                                noAdsAvailable = false
                            )
                        }
                        startPlayback()
                    } else {
                        _uiState.update { it.copy(noAdsAvailable = false) }
                    }
                } else {
                    Log.d(TAG, "Active playlist is empty or cleared.")
                    if (activeMediaList.any { it.isRealAd }) {
                        // Safe transition: Wait for currently playing ad to finish before navigating to NoAds
                        pendingNoAdsTransition = true
                    } else {
                        activeMediaList = emptyList()
                        _uiState.update { it.copy(playlist = emptyList(), currentItem = null, noAdsAvailable = true) }
                    }
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
                            filePath = file.absolutePath,
                            playlistId = playlist.id,
                            playlistItemId = item.id,
                            mediaAssetId = item.media.id,
                            playlistVersion = playlist.version,
                            mediaChecksum = item.media.checksum
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
                            filePath = file.absolutePath,
                            playlistId = playlist.id,
                            playlistItemId = item.id,
                            mediaAssetId = item.media.id,
                            playlistVersion = playlist.version,
                            mediaChecksum = item.media.checksum
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
            val fromStorage = resolveInitialPlaylist()
            if (fromStorage.isNotEmpty()) {
                activeMediaList = fromStorage
            } else {
                activeMediaList = dummyFallbackPlaylist
            }
        }

        val currentItem = activeMediaList.getOrNull(currentIndex)
            ?: activeMediaList.firstOrNull()
            ?: return

        _uiState.update {
            it.copy(
                playlist = activeMediaList,
                currentIndex = currentIndex,
                currentItem = currentItem,
                currentProgress = 0f,
                noAdsAvailable = false
            )
        }

        onAdStarted(currentItem)

        if (startTimer) {
            scheduleCurrentAdTimer(currentItem)
        }
    }

    /**
     * Records an ad start event immediately to the local database.
     */
    private fun onAdStarted(item: AdItem) {
        if (item.isRealAd && item.playlistId != null && item.playlistItemId != null && item.mediaAssetId != null) {
            val eventId = UUID.randomUUID().toString()
            currentPlayingEventId = eventId
            val event = PlaybackEvent(
                eventId = eventId,
                playlistId = item.playlistId!!,
                playlistItemId = item.playlistItemId!!,
                mediaAssetId = item.mediaAssetId!!,
                playlistVersion = item.playlistVersion ?: 1,
                mediaChecksum = item.mediaChecksum ?: "",
                playedAt = getUtcTimestamp(),
                playedCompleteAt = null
            )
            viewModelScope.launch {
                playbackEventRepository?.recordPlaybackStart(event)
            }
        } else {
            currentPlayingEventId = null
        }
    }

    /**
     * Records natural ad completion and checks for pending empty playlist transition.
     */
    private fun onAdCompletedNaturally() {
        val completedId = currentPlayingEventId
        if (completedId != null) {
            val now = getUtcTimestamp()
            viewModelScope.launch {
                playbackEventRepository?.recordPlaybackComplete(completedId, now)
            }
            currentPlayingEventId = null
        }

        if (pendingNoAdsTransition) {
            Log.d(TAG, "Current ad finished. Transitioning to No Ads screen as playlist was cleared.")
            adPlaybackJob?.cancel()
            _uiState.update { it.copy(noAdsAvailable = true) }
            return
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
                onAdCompletedNaturally()
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
            onAdCompletedNaturally()
            advanceToNextItem(startTimer = startTimer)
        }
    }

    /**
     * Called when video playback encounters an error, safely falling back to the next item.
     * Leaves played_complete_at as NULL in the database per requirements.
     */
    fun onVideoError(startTimer: Boolean = true) {
        Log.w(TAG, "Video error encountered at index $currentIndex. Advancing.")
        currentPlayingEventId = null
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

        onAdStarted(nextItem)

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
                            val err = result.error
                            when {
                                err is NetworkError.DeviceRevoked -> {
                                    // Device already revoked or disconnected on backend; wipe local state and exit
                                    Log.d(TAG, "Device was already revoked (${err.errorCode}). Wiping local data.")
                                    clearAllLocalData()
                                    _uiState.update { it.copy(isLoggingOut = false) }
                                    reset()
                                    onSuccess()
                                }
                                err.isNetworkConnectionIssue -> {
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
            // Cancel running playback and reporting jobs
            inactivityJob?.cancel()
            adPlaybackJob?.cancel()

            // Clear in-memory active list and flags
            activeMediaList = emptyList()
            currentIndex = 0
            currentPlayingEventId = null
            pendingNoAdsTransition = false

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

            // Reset UI state cleanly to empty
            _uiState.value = AdsRunningUiState(
                playlist = emptyList(),
                currentIndex = 0,
                currentItem = null,
                currentProgress = 0f,
                noAdsAvailable = false,
                isControlsVisible = true
            )
            Log.d(TAG, "All local credentials, session tokens, and cached media cleared successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing local media or credential storage", e)
        }
    }

    /**
     * Resets the player state, timers, and restores verified ads from storage.
     */
    fun reset() {
        inactivityJob?.cancel()
        adPlaybackJob?.cancel()
        currentIndex = 0
        pendingNoAdsTransition = false
        val fromStorage = resolveInitialPlaylist()
        activeMediaList = fromStorage
        _uiState.value = AdsRunningUiState(
            playlist = activeMediaList,
            currentIndex = 0,
            currentItem = activeMediaList.firstOrNull(),
            currentProgress = 0f,
            noAdsAvailable = false,
            isControlsVisible = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        inactivityJob?.cancel()
        adPlaybackJob?.cancel()
        reportingJob?.cancel()
    }

    companion object {
        private const val TAG = "AdsRunningVM"
    }
}
