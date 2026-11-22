package com.adveno.tv.presentation.screens.syncing

/**
 * UI State for the Ads Synchronization screen.
 * Reflects real backend sync states, progressive download progress, independent retry tracking,
 * error handling, and recovery screen transitions.
 */
data class SyncingUiState(
    val brandName: String = "Adveno",
    val isSynced: Boolean = false,
    val isRevoked: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val progress: Float = 0f,
    val heading: String = if (isSynced) "Synced!" else "Syncing Ads...",
    val description: String = if (isSynced) {
        "Your advertisements are ready."
    } else {
        "Downloading the latest content to this screen."
    },
    val recoveryTitle: String = "Unable to synchronize your content.",
    val recoveryMessage: String = "Please contact Adveno support or continue showing existing ads.",
    val errorMessage: String? = null,
    val currentAttempt: Int = 1,
    val maxAttempts: Int = 3,
    val isAutoRetrying: Boolean = false
) {
    val percentageText: String
        get() = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"

    val attemptIndicatorText: String
        get() = "Attempt $currentAttempt of $maxAttempts"
}
