package com.adveno.tv.data.remote.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for assigned screen playlist (GET /screens/assigned-playlist).
 *
 * Backend can return:
 * 1. `{ "playlist": null, "items": [] }` when no Ads are assigned.
 * 2. `{ "playlist": { "id": "...", "name": "...", "version": 6 }, "items": [ ... ] }` with nested playlist object.
 * 3. `{ "id": "...", "name": "...", "version": 6, "items": [ ... ] }` with flattened playlist fields.
 * 4. Raw HTTP 200 `null` body.
 */
@Serializable
data class AssignedScreenPlaylistResponse(
    @SerialName("id")
    val id: String? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("version")
    val version: Int = 0,

    @SerialName("playlist")
    val playlist: PlaylistDetailsResponse? = null,

    @SerialName("items")
    val items: List<PlaylistItemResponse> = emptyList()
)

@Serializable
data class PlaylistDetailsResponse(
    @SerialName("id")
    val id: String? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("version")
    val version: Int = 0
)
