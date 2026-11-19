package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for polling pairing session status (GET /pairing/sessions/{session_id}).
 * Allowed status values: PENDING, CONNECTING, PAIRED, EXPIRED.
 */
@Serializable
data class ScreenPairingSessionStatusResponse(
    @SerialName("status")
    val status: String
)

