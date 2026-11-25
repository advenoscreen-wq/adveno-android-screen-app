package com.adveno.tv.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyAdminPinRequest(
    @SerialName("pin")
    val pin: String
)
