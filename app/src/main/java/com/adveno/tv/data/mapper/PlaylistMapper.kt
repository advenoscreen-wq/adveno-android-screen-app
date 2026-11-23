package com.adveno.tv.data.mapper

import com.adveno.tv.data.remote.dto.response.AssignedScreenPlaylistResponse
import com.adveno.tv.data.remote.dto.response.MediaResponse
import com.adveno.tv.data.remote.dto.response.PlaylistItemResponse
import com.adveno.tv.domain.model.media.Media
import com.adveno.tv.domain.model.media.MediaStatus
import com.adveno.tv.domain.model.media.MediaType
import com.adveno.tv.domain.model.playlist.Playlist
import com.adveno.tv.domain.model.playlist.PlaylistItem

object PlaylistMapper {

    fun toDomain(response: AssignedScreenPlaylistResponse?): Playlist? {
        if (response == null) return null

        val playlistId = response.id ?: response.playlist?.id
        val playlistName = response.name ?: response.playlist?.name
        val description = response.description ?: response.playlist?.description
        val version = if (response.version != 0) response.version else response.playlist?.version ?: 0

        // If no playlist information or no items are assigned, return null as a valid empty state
        if (playlistId.isNullOrBlank() || playlistName.isNullOrBlank() || response.items.isEmpty()) {
            return null
        }

        return Playlist(
            id = playlistId,
            name = playlistName,
            description = description,
            version = version,
            items = response.items
                .sortedBy { it.position }
                .map { toDomain(it) }
        )
    }

    fun toDomain(dto: PlaylistItemResponse): PlaylistItem {
        val duration = dto.durationSeconds ?: dto.duration ?: dto.media.durationSeconds ?: dto.media.duration

        return PlaylistItem(
            id = dto.id,
            position = dto.position,
            durationSeconds = duration,
            media = toDomain(dto.media)
        )
    }

    fun toDomain(dto: MediaResponse): Media {
        val type = when (dto.mediaType.uppercase()) {
            "VIDEO" -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }

        val status = when (dto.status.uppercase()) {
            "DELETED" -> MediaStatus.DELETED
            else -> MediaStatus.ACTIVE
        }

        val duration = dto.durationSeconds ?: dto.duration

        return Media(
            id = dto.id,
            name = dto.name,
            mediaType = type,
            mimeType = dto.mimeType,
            storagePath = dto.storagePath,
            fileSize = dto.fileSize,
            checksum = dto.checksum,
            width = dto.width,
            height = dto.height,
            durationSeconds = duration,
            status = status,
            mediaUrl = dto.mediaUrl,
            localFilePath = null
        )
    }
}
