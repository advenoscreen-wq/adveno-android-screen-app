package com.adveno.tv.domain.model.playlist

import com.adveno.tv.domain.model.media.Media
import kotlinx.serialization.Serializable

/**
 * Domain entity representing an item inside an assigned playlist.
 */
@Serializable
data class PlaylistItem(
    val id: String,
    val position: Int,
    val durationSeconds: Int? = null,
    val media: Media
)

