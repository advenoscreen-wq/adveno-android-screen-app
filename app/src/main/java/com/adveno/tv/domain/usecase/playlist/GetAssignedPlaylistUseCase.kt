package com.adveno.tv.domain.usecase.playlist

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.playlist.Playlist
import com.adveno.tv.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class GetAssignedPlaylistUseCase(
    private val repository: PlaylistRepository
) {
    operator fun invoke(): Flow<NetworkResult<Playlist?>> {
        return repository.getAssignedPlaylist()
    }
}

