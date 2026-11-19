package com.adveno.tv.domain.model.playlist

/**
 * Domain entity representing an assigned screen playlist.
 */
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: Int,
    val items: List<PlaylistItem> = emptyList()
)

