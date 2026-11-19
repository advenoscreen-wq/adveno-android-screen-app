package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for sync check / heartbeat (POST /screens/sync-check).
 */
@Serializable
data class ScreenSyncCheckResponse(
    @SerialName("need_sync")
    val needSync: Boolean,

    @SerialName("desired_revision")
    val desiredRevision: Int? = null
)
