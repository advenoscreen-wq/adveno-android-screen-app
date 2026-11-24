package com.adveno.tv.data.remote.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackEventDto(
    @SerialName("event_id")
    val eventId: String,

    @SerialName("playlist_id")
    val playlistId: String,

    @SerialName("playlist_item_id")
    val playlistItemId: String,

    @SerialName("media_asset_id")
    val mediaAssetId: String,

    @SerialName("playlist_version")
    val playlistVersion: Int,

    @SerialName("media_checksum")
    val mediaChecksum: String,

    @SerialName("played_at")
    val playedAt: String,

    @SerialName("played_complete_at")
    val playedCompleteAt: String? = null
)

@Serializable
data class PlaybackEventBatchRequest(
    @SerialName("events")
    val events: List<PlaybackEventDto>
)
