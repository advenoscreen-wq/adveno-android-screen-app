package com.adveno.tv.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.adveno.tv.data.local.PlaybackEventDatabaseHelper
import com.adveno.tv.domain.model.event.PlaybackEvent
import com.adveno.tv.domain.repository.PlaybackEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackEventRepositoryImpl(
    private val dbHelper: PlaybackEventDatabaseHelper
) : PlaybackEventRepository {

    override suspend fun recordPlaybackStart(event: PlaybackEvent): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(PlaybackEventDatabaseHelper.COLUMN_EVENT_ID, event.eventId)
                put(PlaybackEventDatabaseHelper.COLUMN_PLAYLIST_ID, event.playlistId)
                put(PlaybackEventDatabaseHelper.COLUMN_PLAYLIST_ITEM_ID, event.playlistItemId)
                put(PlaybackEventDatabaseHelper.COLUMN_MEDIA_ASSET_ID, event.mediaAssetId)
                put(PlaybackEventDatabaseHelper.COLUMN_PLAYLIST_VERSION, event.playlistVersion)
                put(PlaybackEventDatabaseHelper.COLUMN_MEDIA_CHECKSUM, event.mediaChecksum)
                put(PlaybackEventDatabaseHelper.COLUMN_PLAYED_AT, event.playedAt)
                put(PlaybackEventDatabaseHelper.COLUMN_PLAYED_COMPLETE_AT, event.playedCompleteAt)
            }
            db.insertWithOnConflict(
                PlaybackEventDatabaseHelper.TABLE_NAME,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
            Log.d(TAG, "Recorded playback start: ${event.eventId} for item ${event.playlistItemId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording playback start for ${event.eventId}", e)
        }
    }

    override suspend fun recordPlaybackComplete(eventId: String, completedAtUtc: String): Unit = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(PlaybackEventDatabaseHelper.COLUMN_PLAYED_COMPLETE_AT, completedAtUtc)
            }
            db.update(
                PlaybackEventDatabaseHelper.TABLE_NAME,
                values,
                "${PlaybackEventDatabaseHelper.COLUMN_EVENT_ID} = ?",
                arrayOf(eventId)
            )
            Log.d(TAG, "Recorded playback completion: $eventId at $completedAtUtc")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording playback completion for $eventId", e)
        }
    }

    override suspend fun getPendingEvents(excludeEventId: String?, limit: Int): List<PlaybackEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<PlaybackEvent>()
        val db = dbHelper.readableDatabase

        val selection: String?
        val selectionArgs: Array<String>?
        if (!excludeEventId.isNullOrBlank()) {
            selection = "${PlaybackEventDatabaseHelper.COLUMN_EVENT_ID} != ?"
            selectionArgs = arrayOf(excludeEventId)
        } else {
            selection = null
            selectionArgs = null
        }

        var cursor: Cursor? = null
        try {
            cursor = db.query(
                PlaybackEventDatabaseHelper.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "${PlaybackEventDatabaseHelper.COLUMN_PLAYED_AT} ASC",
                limit.toString()
            )

            if (cursor.moveToFirst()) {
                val eventIdIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_EVENT_ID)
                val playlistIdIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_PLAYLIST_ID)
                val playlistItemIdIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_PLAYLIST_ITEM_ID)
                val mediaAssetIdIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_MEDIA_ASSET_ID)
                val playlistVersionIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_PLAYLIST_VERSION)
                val mediaChecksumIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_MEDIA_CHECKSUM)
                val playedAtIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_PLAYED_AT)
                val playedCompleteAtIdx = cursor.getColumnIndexOrThrow(PlaybackEventDatabaseHelper.COLUMN_PLAYED_COMPLETE_AT)

                do {
                    events.add(
                        PlaybackEvent(
                            eventId = cursor.getString(eventIdIdx),
                            playlistId = cursor.getString(playlistIdIdx),
                            playlistItemId = cursor.getString(playlistItemIdIdx),
                            mediaAssetId = cursor.getString(mediaAssetIdIdx),
                            playlistVersion = cursor.getInt(playlistVersionIdx),
                            mediaChecksum = cursor.getString(mediaChecksumIdx),
                            playedAt = cursor.getString(playedAtIdx),
                            playedCompleteAt = if (cursor.isNull(playedCompleteAtIdx)) null else cursor.getString(playedCompleteAtIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending playback events", e)
        } finally {
            cursor?.close()
        }

        events
    }

    override suspend fun deleteEvents(eventIds: List<String>): Unit = withContext(Dispatchers.IO) {
        if (eventIds.isEmpty()) return@withContext
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (id in eventIds) {
                db.delete(
                    PlaybackEventDatabaseHelper.TABLE_NAME,
                    "${PlaybackEventDatabaseHelper.COLUMN_EVENT_ID} = ?",
                    arrayOf(id)
                )
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Deleted ${eventIds.size} reported playback events from local database.")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reported events", e)
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun getPendingEventCount(excludeEventId: String?): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val selection = if (!excludeEventId.isNullOrBlank()) {
            "${PlaybackEventDatabaseHelper.COLUMN_EVENT_ID} != ?"
        } else null
        val selectionArgs = if (!excludeEventId.isNullOrBlank()) arrayOf(excludeEventId) else null

        var cursor: Cursor? = null
        try {
            cursor = db.query(
                PlaybackEventDatabaseHelper.TABLE_NAME,
                arrayOf("COUNT(*)"),
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            if (cursor.moveToFirst()) {
                return@withContext cursor.getInt(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying pending event count", e)
        } finally {
            cursor?.close()
        }
        return@withContext 0
    }

    companion object {
        private const val TAG = "PlaybackEventRepo"
    }
}
