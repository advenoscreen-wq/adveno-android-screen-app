package com.adveno.tv.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for acknowledging synchronization (POST /screens/sync-acknowledgments).
 * Confirms that the TV screen has successfully downloaded and applied the given revision.
 */
@Serializable
data class ScreenSyncAcknowledgmentRequest(
    @SerialName("applied_revision")
    val appliedRevision: Int
)

