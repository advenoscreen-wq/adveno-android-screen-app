package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Playlist item response payload.
 */
@Serializable
data class PlaylistItemResponse(
    @SerialName("id")
    val id: String,

    @SerialName("position")
    val position: Int,

    @SerialName("duration_seconds")
    val durationSeconds: Int? = null,

    @SerialName("media")
    val media: MediaResponse
)

