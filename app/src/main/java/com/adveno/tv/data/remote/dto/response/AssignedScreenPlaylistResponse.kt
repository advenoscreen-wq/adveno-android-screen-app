package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for assigned screen playlist (GET /screens/assigned-playlist).
 * Note: The entire response body may be JSON `null` if no playlist is currently assigned.
 */
@Serializable
data class AssignedScreenPlaylistResponse(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("version")
    val version: Int,

    @SerialName("items")
    val items: List<PlaylistItemResponse> = emptyList()
)

