package com.adveno.tv.core.storage

import android.content.Context
import android.util.Log
import com.adveno.tv.domain.model.playlist.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Thread-safe persistent storage holding the currently applied playlist and verified local media.
 * Restores the last successfully applied playlist across process restarts and offline reboots.
 */
class ActivePlaylistStorage(
    private val context: Context? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val playlistFile: File?
        get() = context?.let { File(it.filesDir, FILE_NAME) }

    private val prefs by lazy {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    private val _pendingAcknowledgmentRevision = MutableStateFlow<Int?>(null)
    val pendingAcknowledgmentRevision: StateFlow<Int?> = _pendingAcknowledgmentRevision.asStateFlow()

    init {
        restorePersistedPlaylist()
    }

    /**
     * Inspects local disk storage on startup and restores the assigned playlist and media references.
     */
    private fun restorePersistedPlaylist() {
        val file = playlistFile ?: return
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            Log.d(TAG, "No persisted playlist file found on disk.")
            return
        }

        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val restored = json.decodeFromString<Playlist>(content)
                // Verify that referenced media files still exist on disk
                val hasValidMedia = restored.items.isNotEmpty() && restored.items.all { item ->
                    val path = item.media.localFilePath
                    if (path.isNullOrBlank()) false else {
                        val mediaFile = File(path)
                        mediaFile.exists() && mediaFile.canRead() && mediaFile.length() > 0L
                    }
                }

                if (hasValidMedia) {
                    _currentPlaylist.value = restored
                    Log.d(TAG, "Successfully restored persisted playlist '${restored.name}' (v${restored.version}) with ${restored.items.size} verified items from disk.")
                } else {
                    Log.w(TAG, "Persisted playlist '${restored.name}' found, but some media files are missing on disk.")
                    val validItems = restored.items.filter { item ->
                        val path = item.media.localFilePath
                        !path.isNullOrBlank() && File(path).let { it.exists() && it.canRead() && it.length() > 0L }
                    }
                    if (validItems.isNotEmpty()) {
                        val usablePlaylist = restored.copy(items = validItems)
                        _currentPlaylist.value = usablePlaylist
                        Log.d(TAG, "Restored playlist with ${validItems.size}/${restored.items.size} verified media files.")
                    } else {
                        Log.w(TAG, "All media files for persisted playlist missing. Not restoring.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing persisted playlist", e)
        }

        // Restore pending acknowledgment revision if any
        val savedRev = prefs?.getInt(KEY_PENDING_ACK_REVISION, -1) ?: -1
        if (savedRev != -1) {
            _pendingAcknowledgmentRevision.value = savedRev
        }
    }

    fun setPlaylist(playlist: Playlist?) {
        _currentPlaylist.value = playlist
        persistToDisk(playlist)
    }

    fun getPlaylist(): Playlist? {
        return _currentPlaylist.value
    }

    fun setPendingAcknowledgment(revision: Int?) {
        _pendingAcknowledgmentRevision.value = revision
        prefs?.edit()?.apply {
            if (revision != null) {
                putInt(KEY_PENDING_ACK_REVISION, revision)
            } else {
                remove(KEY_PENDING_ACK_REVISION)
            }
        }?.apply()
    }

    fun getPendingAcknowledgment(): Int? = _pendingAcknowledgmentRevision.value

    fun hasUsablePlaylist(): Boolean {
        val playlist = _currentPlaylist.value ?: return false
        if (playlist.items.isEmpty()) return false
        return playlist.items.all { item ->
            val path = item.media.localFilePath
            !path.isNullOrBlank() && File(path).let { it.exists() && it.canRead() && it.length() > 0L }
        }
    }

    fun clearPlaylist() {
        _currentPlaylist.value = null
        _pendingAcknowledgmentRevision.value = null
        persistToDisk(null)
        prefs?.edit()?.remove(KEY_PENDING_ACK_REVISION)?.apply()
    }

    private fun persistToDisk(playlist: Playlist?) {
        val file = playlistFile ?: return
        try {
            if (playlist == null) {
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Persisted playlist file deleted.")
                }
            } else {
                val parent = file.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }
                val serialized = json.encodeToString(Playlist.serializer(), playlist)
                val tempFile = File(parent, "${FILE_NAME}.tmp")
                tempFile.writeText(serialized)
                if (tempFile.renameTo(file)) {
                    Log.d(TAG, "Persisted active playlist '${playlist.name}' (v${playlist.version}) to disk successfully.")
                } else {
                    file.writeText(serialized)
                    tempFile.delete()
                    Log.d(TAG, "Persisted active playlist '${playlist.name}' (v${playlist.version}) via direct write.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist active playlist to disk", e)
        }
    }

    companion object {
        private const val TAG = "ActivePlaylistStorage"
        private const val FILE_NAME = "active_playlist.json"
        private const val PREFS_NAME = "adveno_active_playlist_prefs"
        private const val KEY_PENDING_ACK_REVISION = "pending_ack_revision"
    }
}
