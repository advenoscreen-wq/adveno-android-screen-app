package com.adveno.tv.domain.repository

import com.adveno.tv.core.network.NetworkResult
import com.adveno.tv.domain.model.playlist.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for retrieving assigned advertisement playlists.
 */
interface PlaylistRepository {

    /**
     * Fetches the currently assigned playlist for this screen device.
     * Returns `null` if no playlist is currently assigned.
     */
    fun getAssignedPlaylist(): Flow<NetworkResult<Playlist?>>
}

