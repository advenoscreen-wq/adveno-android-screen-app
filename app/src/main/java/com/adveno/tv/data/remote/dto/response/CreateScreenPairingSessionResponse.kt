package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for POST /pairing/sessions (HTTP 201 Created).
 */
@Serializable
data class CreateScreenPairingSessionResponse(
    @SerialName("session_id")
    val sessionId: String,

    @SerialName("pairing_token")
    val pairingToken: String,

    @SerialName("expires_at")
    val expiresAt: String
)

