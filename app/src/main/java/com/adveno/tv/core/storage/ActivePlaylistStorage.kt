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

    private val _pendingAcknowledgmentRevision = MutableStateFlow<Int?>(null)
    val pendingAcknowledgmentRevision: StateFlow<Int?> = _pendingAcknowledgmentRevision.asStateFlow()

    fun setPlaylist(playlist: Playlist?) {
        _currentPlaylist.value = playlist
    }

    fun getPlaylist(): Playlist? {
        return _currentPlaylist.value
    }

    fun setPendingAcknowledgment(revision: Int?) {
        _pendingAcknowledgmentRevision.value = revision
    }

    fun getPendingAcknowledgment(): Int? = _pendingAcknowledgmentRevision.value

    fun hasUsablePlaylist(): Boolean {
        val playlist = _currentPlaylist.value
        return playlist != null && playlist.items.isNotEmpty() && playlist.items.all {
            it.media.localFilePath != null
        }
    }

    fun clearPlaylist() {
        _currentPlaylist.value = null
        _pendingAcknowledgmentRevision.value = null
    }
}

