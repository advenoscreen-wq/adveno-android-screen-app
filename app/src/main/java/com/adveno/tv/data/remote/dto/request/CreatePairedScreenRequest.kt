package com.adveno.tv.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for registering a new screen (POST /pairing/sessions/{session_id}/screen).
 * Requires App-User Supabase JWT in Authorization header alongside X-Pairing-Token.
 */
@Serializable
data class CreatePairedScreenRequest(
    @SerialName("name")
    val name: String,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null
)

