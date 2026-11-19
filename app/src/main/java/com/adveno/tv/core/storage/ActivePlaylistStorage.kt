package com.adveno.tv.core.storage

import com.adveno.tv.domain.model.playlist.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared in-memory storage holding the currently applied playlist and verified local media.
 * Accessible to the digital signage playback engine.
 */
class ActivePlaylistStorage {

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    fun setPlaylist(playlist: Playlist?) {
        _currentPlaylist.value = playlist
    }

    fun getPlaylist(): Playlist? {
        return _currentPlaylist.value
    }

    fun hasUsablePlaylist(): Boolean {
        val playlist = _currentPlaylist.value
        return playlist != null && playlist.items.isNotEmpty() && playlist.items.all {
            it.media.localFilePath != null
        }
    }

    fun clearPlaylist() {
        _currentPlaylist.value = null
    }
}

