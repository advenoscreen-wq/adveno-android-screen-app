package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyAdminPinResponse(
    @SerialName("session_token")
    val sessionToken: String? = null,

    @SerialName("token")
    val token: String? = null,

    @SerialName("admin_session_token")
    val adminSessionToken: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null
) {
    val rawToken: String?
        get() = sessionToken ?: token ?: adminSessionToken
}
