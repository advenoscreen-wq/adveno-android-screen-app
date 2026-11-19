package com.adveno.tv.presentation.screens.syncing

/**
 * UI State for the Ads Synchronization screen.
 * Reflects real backend sync states, progressive download progress, error handling, and completion.
 */
data class SyncingUiState(
    val brandName: String = "Adveno",
    val isSynced: Boolean = false,
    val isRevoked: Boolean = false,
    val progress: Float = 0f,
    val heading: String = if (isSynced) "Synced!" else "Syncing Ads...",
    val description: String = if (isSynced) {
        "Your advertisements are ready."
    } else {
        "Downloading the latest content to this screen."
    },
    val errorMessage: String? = null,
    val canRetry: Boolean = false
) {
    val percentageText: String
        get() = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
}
