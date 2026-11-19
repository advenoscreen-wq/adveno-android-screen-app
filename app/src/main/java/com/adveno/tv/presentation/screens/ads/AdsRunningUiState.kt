package com.adveno.tv.presentation.screens.ads

/**
 * UI State for the Ads Running digital signage player.
 *
 * @property playlist The configured list of advertisement items that loop indefinitely.
 * @property currentIndex The 0-based index of the currently active media item.
 * @property currentItem The active advertisement item (Video or Image) being displayed.
 * @property currentProgress Playback progress of the CURRENT ad item (0.0f to 1.0f).
 * @property isControlsVisible Whether overlay controls (Logout and Sync buttons) are visible.
 */
data class AdsRunningUiState(
    val playlist: List<AdItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentItem: AdItem? = null,
    val currentProgress: Float = 0f,
    val isControlsVisible: Boolean = true,
    val isLoggingOut: Boolean = false,
    val errorMessage: String? = null
) {
    val totalItems: Int get() = playlist.size
    val isVideoPlaying: Boolean get() = currentItem is AdItem.Video
    val isImagePlaying: Boolean get() = currentItem is AdItem.Image
}
