package com.adveno.tv.presentation.screens.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adveno.tv.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for the digital signage advertisement player:
 * - Manages playlist loop: Video (10s) -> Image (5s) -> Video (10s) -> Image (5s) indefinitely.
 * - Synchronizes current ad progress (0% to 100%) for both video and image items.
 * - Handles auto-hide overlay controls after 3 seconds of user inactivity.
 */
class AdsRunningViewModel(
    customPlaylist: List<AdItem>? = null
) : ViewModel() {

    // Defined media playlist for digital signage looping
    val mediaPlaylist: List<AdItem> = customPlaylist ?: listOf(
        AdItem.Video(
            id = "ad_item_video_1",
            title = "Adveno Video Ad (10s)",
            durationMs = 10_000L,
            rawResId = R.raw.sample_ad_video
        ),
        AdItem.Image(
            id = "ad_item_image_2",
            title = "Adveno Display Banner (5s)",
            durationMs = 5_000L,
            drawableResId = R.drawable.sample_ad_image
        )
    )

    private var currentIndex = 0
    private var inactivityJob: Job? = null
    private var adPlaybackJob: Job? = null

    private val _uiState = MutableStateFlow(
        AdsRunningUiState(
            playlist = mediaPlaylist,
            currentIndex = 0,
            currentItem = mediaPlaylist.firstOrNull(),
            currentProgress = 0f,
            isControlsVisible = true
        )
    )
    val uiState: StateFlow<AdsRunningUiState> = _uiState.asStateFlow()

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
        val currentItem = mediaPlaylist.getOrNull(currentIndex) ?: return
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
     * Internal coroutine timer driving the current item duration (10s video / 5s image).
     */
    private fun scheduleCurrentAdTimer(item: AdItem) {
        adPlaybackJob?.cancel()
        adPlaybackJob = viewModelScope.launch {
            val totalDuration = if (item.durationMs > 0L) item.durationMs else 10_000L
            val stepInterval = 50L
            val totalSteps = (totalDuration / stepInterval).toInt().coerceAtLeast(1)

            for (step in 1..totalSteps) {
                delay(stepInterval)
                val progress = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
                _uiState.update { it.copy(currentProgress = progress) }
            }

            // Duration completed -> loop to the next item
            advanceToNextItem(startTimer = true)
        }
    }

    /**
     * Fine-tunes the playback progress if accurate position is provided by the media player.
     */
    fun updateVideoProgress(positionMs: Long, durationMs: Long) {
        if (_uiState.value.currentItem !is AdItem.Video) return
        val safeDuration = if (durationMs > 0L) durationMs else 10_000L
        val progress = (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
        _uiState.update { it.copy(currentProgress = progress) }
    }

    /**
     * Called when the video ad reaches its end.
     */
    fun onVideoCompleted(startTimer: Boolean = true) {
        if (_uiState.value.currentItem is AdItem.Video) {
            advanceToNextItem(startTimer = startTimer)
        }
    }

    /**
     * Called when video playback encounters an issue or error, safely falling back to the next item.
     */
    fun onVideoError(startTimer: Boolean = true) {
        advanceToNextItem(startTimer = startTimer)
    }

    /**
     * Advances to the next item in the playlist and manages the indefinite loop.
     */
    fun advanceToNextItem(startTimer: Boolean = true) {
        if (mediaPlaylist.isEmpty()) return

        adPlaybackJob?.cancel()
        currentIndex = (currentIndex + 1) % mediaPlaylist.size
        val nextItem = mediaPlaylist[currentIndex]

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
     * Resets the player state, timers, and returns to the first item.
     */
    fun reset() {
        inactivityJob?.cancel()
        adPlaybackJob?.cancel()
        currentIndex = 0
        _uiState.value = AdsRunningUiState(
            playlist = mediaPlaylist,
            currentIndex = 0,
            currentItem = mediaPlaylist.firstOrNull(),
            currentProgress = 0f,
            isControlsVisible = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        inactivityJob?.cancel()
        adPlaybackJob?.cancel()
    }
}
