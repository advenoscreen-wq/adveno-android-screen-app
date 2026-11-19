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

        return Playlist(
            id = response.id,
            name = response.name,
            description = response.description,
            version = response.version,
            items = response.items
                .sortedBy { it.position }
                .map { toDomain(it) }
        )
    }

    fun toDomain(dto: PlaylistItemResponse): PlaylistItem {
        return PlaylistItem(
            id = dto.id,
            position = dto.position,
            durationSeconds = dto.durationSeconds,
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
            durationSeconds = dto.durationSeconds,
            status = status,
            mediaUrl = dto.mediaUrl
        )
    }
}

