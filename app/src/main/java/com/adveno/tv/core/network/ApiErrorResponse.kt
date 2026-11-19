package com.adveno.tv.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Encapsulates error structures returned by the FastAPI backend.
 * Handles both:
 * 1. Application-level domain errors:
 *    {
 *      "error": {
 *        "code": "PAIRING_SESSION_NOT_FOUND",
 *        "message": "Pairing session was not found.",
 *        "status_code": 404
 *      }
 *    }
 * 2. FastAPI validation error payloads:
 *    {
 *      "detail": [...]
 *    }
 */
@Serializable
data class ApiErrorResponse(
    @SerialName("error")
    val appError: AppErrorDetail? = null,

    @SerialName("detail")
    val validationDetail: JsonElement? = null
)

@Serializable
data class AppErrorDetail(
    @SerialName("code")
    val code: String? = null,

    @SerialName("message")
    val message: String? = null,

    @SerialName("status_code")
    val statusCode: Int? = null
)

