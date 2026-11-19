package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend response DTO for screen credential verification (POST /api/v1/screens/credential/verify).
 */
@Serializable
data class VerifyScreenCredentialResponse(
    @SerialName("valid")
    val valid: Boolean = false
)

