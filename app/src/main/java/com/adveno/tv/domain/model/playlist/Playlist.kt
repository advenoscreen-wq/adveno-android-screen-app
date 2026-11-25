package com.adveno.tv.domain.model.playlist

import kotlinx.serialization.Serializable

/**
 * Domain entity representing an assigned screen playlist.
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: Int,
    val items: List<PlaylistItem> = emptyList()
)

