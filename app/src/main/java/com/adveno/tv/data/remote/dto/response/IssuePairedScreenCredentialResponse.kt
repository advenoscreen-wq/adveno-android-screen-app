package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for issuing reconnect credentials (POST /pairing/sessions/{session_id}/credential).
 */
@Serializable
data class IssuePairedScreenCredentialResponse(
    @SerialName("screen_id")
    val screenId: String,

    @SerialName("credential")
    val credential: String
)

