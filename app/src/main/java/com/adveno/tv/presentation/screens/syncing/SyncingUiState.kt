package com.adveno.tv.presentation.screens.syncing

/**
 * UI State for the Ads Synchronization screen.
 * Reflects real backend sync states, progressive download progress, independent retry tracking,
 * network disconnection modal state, error handling, and recovery screen transitions.
 */
data class SyncingUiState(
    val brandName: String = "Adveno",
    val isSynced: Boolean = false,
    val isRevoked: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val hasAdsAvailable: Boolean = false,
    val hasUsableLocalAds: Boolean = false,
    val isManualSyncFailed: Boolean = false,
    val progress: Float = 0f,
    val heading: String = if (isSynced) "Synced!" else "Syncing Ads...",
    val description: String = if (isSynced) {
        "Your Ads are ready."
    } else {
        "Downloading the latest Ads to this screen."
    },
    val recoveryTitle: String = "Unable to synchronize your Ads.",
    val recoveryMessage: String = "Please contact Adveno support or sync to retry.",
    val errorMessage: String? = null,
    val currentAttempt: Int = 1,
    val maxAttempts: Int = 3,
    val isAutoRetrying: Boolean = false,
    val isNoInternetModalVisible: Boolean = false,
    val interruptedOperation: String = "",
    val noInternetStatusMessage: String? = null,
    val isConnectionRestored: Boolean = false
) {
    val percentageText: String
        get() = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
}
