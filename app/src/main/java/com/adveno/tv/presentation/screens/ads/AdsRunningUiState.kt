package com.adveno.tv.presentation.screens.ads

/**
 * UI State for the Ads Running digital signage player.
 *
 * @property currentItem The active advertisement item (Video or Image) being displayed.
 * @property currentProgress Playback progress of the CURRENT ad item (0.0f to 1.0f).
 * @property isControlsVisible Whether overlay controls (Logout and Sync buttons) are visible.
 */
data class AdsRunningUiState(
    val currentItem: AdItem? = null,
    val currentProgress: Float = 0f,
    val isControlsVisible: Boolean = true
)

