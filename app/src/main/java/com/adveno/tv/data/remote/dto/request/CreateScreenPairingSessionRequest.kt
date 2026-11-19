package com.adveno.tv.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for creating a screen pairing session (POST /pairing/sessions).
 */
@Serializable
data class CreateScreenPairingSessionRequest(
    @SerialName("pairing_type")
    val pairingType: String
)

