package com.adveno.tv.domain.model.event

/**
 * Domain entity representing an ad playback record for auditing and analytics.
 */
data class PlaybackEvent(
    val eventId: String,
    val playlistId: String,
    val playlistItemId: String,
    val mediaAssetId: String,
    val playlistVersion: Int,
    val mediaChecksum: String,
    val playedAt: String,
    val playedCompleteAt: String? = null
)
