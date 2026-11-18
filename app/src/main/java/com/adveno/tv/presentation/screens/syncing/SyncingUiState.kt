package com.adveno.tv.presentation.screens.syncing

/**
 * UI State for the Ads Synchronization screen.
 * Covers both the 5-second "Syncing Ads..." progress state and the final "Synced!" success state.
 */
data class SyncingUiState(
    val brandName: String = "Adveno",
    val isSynced: Boolean = false,
    val progress: Float = 0f,
    val heading: String = if (isSynced) "Synced!" else "Syncing Ads...",
    val description: String = if (isSynced) {
        "Your advertisements are ready."
    } else {
        "Downloading the latest content to this screen."
    }
) {
    val percentageText: String
        get() = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
}

