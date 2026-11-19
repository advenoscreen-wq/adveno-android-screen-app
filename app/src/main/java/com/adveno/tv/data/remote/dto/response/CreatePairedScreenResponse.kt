package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for new screen registration (POST /pairing/sessions/{session_id}/screen).
 */
@Serializable
data class CreatePairedScreenResponse(
    @SerialName("screen_id")
    val screenId: String,

    @SerialName("screen_code")
    val screenCode: String,

    @SerialName("name")
    val name: String,

    @SerialName("credential")
    val credential: String
)

